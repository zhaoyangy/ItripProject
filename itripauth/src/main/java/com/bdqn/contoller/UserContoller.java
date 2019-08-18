package com.bdqn.contoller;

import cn.itrip.common.*;
import cn.itrip.dao.itripUser.ItripUserMapper;
import cn.itrip.pojo.ItripUser;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.bdqn.biz.SentSSM;
import com.bdqn.biz.TokenBiz;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Calendar;
import java.util.Random;

@Controller
@RequestMapping("/api")
@Api(value = "api",description = "爱旅行用户模块")
public class UserContoller {

    @Resource
    ItripUserMapper dao;

    @Resource
    TokenBiz TokenBiz;

    @Resource
    JredisApi JredisApi;

    @Resource
    ItripUserMapper userdao;

    @Resource
    SentSSM sentSSM;


    /**
     * 手机验证
     * @param user
     * @param code
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/validatephone")
    @ResponseBody
    public Dto validatephone(String user,String code) throws Exception {
      try {
          //去redis中查找code数据
          String result= JredisApi.GetRedis("Code:"+user);
          if(result.equals(code)){
              //根据手机号查询实体类 然后修改
              //ItripUser tt=dao.getUesrE(user);
              dao.updateById(user);
              return DtoUtil.returnSuccess("激活成功!!");
          }
      }catch (Exception e){
          return DtoUtil.returnSuccess("激活失败");
      }
          return DtoUtil.returnSuccess("激活失败");
    }


    /**
     * 手机注册
     * @param itripUser
     * @return
     * @throws Exception
     */
    @RequestMapping(value="/registerbyphone",method = RequestMethod.POST)
    @ResponseBody
    public Dto   registerbyphone (@RequestBody ItripUser itripUser) throws Exception {
        try {
            //为手机发送验证码
            //生成4位随机数
            int i= (int)(Math.random()*(9999-1000+1))+1000;

            //把随机的4位数字发送给手机,并发保存到Redis中去
            sentSSM.setPhone(itripUser.getUserCode(),""+i);

            //存到Redis中
            JredisApi.SetRedis("Code:"+itripUser.getUserCode(),""+i,60);
            //把实体类存到数据库中
            ItripUser user =new ItripUser();
            user.setUserCode(itripUser.getUserCode());
            user.setUserName(itripUser.getUserName());
            user.setUserPassword(MD5.getMd5(itripUser.getUserPassword(),32));
            user.setActivated(0);

            Integer result= userdao.insertItripUser(user);
            if(result>0){
                return DtoUtil.returnDataSuccess(user);
            }
        }catch (Exception e){
            return DtoUtil.returnFail("注册失败","1000");
        }
        return DtoUtil.returnFail("注册失败","1000");
    }

    /**
     * 登录
     * @param request
     * @param name
     * @param password
     * @return
     * @throws Exception
     */
    @RequestMapping(value="/dologin",method = RequestMethod.POST)
    @ResponseBody
    @ApiImplicitParams({
            @ApiImplicitParam(value = "用户名",required = true,paramType = "form"),
            @ApiImplicitParam(value = "密码",required = true,paramType = "form"),
    })
    public Dto Getlist(HttpServletRequest request, String name, String password) throws Exception {
        try {
            //判断数据是否存在
            ItripUser itripUser = dao.ifLogin(name,MD5.getMd5(password,32));
            //把标识存进redis
            if (itripUser != null) {
                String token = TokenBiz.generateToken(request.getHeader("User-Agent"), itripUser);

                JredisApi.SetRedis(token, JSONObject.toJSONString(itripUser),60 * 60 * 2);

                ItripTokenVO tokenVO = new ItripTokenVO(token, Calendar.getInstance().getTimeInMillis()*3600*2, Calendar.getInstance().getTimeInMillis());
                return DtoUtil.returnDataSuccess(tokenVO);
            }
        } catch (Exception e) {

        }
        return DtoUtil.returnFail("登录失败","1000");
    }

}
