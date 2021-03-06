package com.ruoyi.fangyuanapi.controller;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.PageConf;
import com.ruoyi.common.redis.config.RedisKeyConf;
import com.ruoyi.common.redis.config.RedisTimeConf;
import com.ruoyi.common.redis.util.RedisUtils;
import com.ruoyi.common.utils.HttpUtil;
import com.ruoyi.common.utils.IpUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.aes.TokenUtils;
import com.ruoyi.common.utils.md5.ZhaoMD5Utils;
import com.ruoyi.common.utils.sms.CategoryType;
import com.ruoyi.common.utils.sms.PhoneUtils;
import com.ruoyi.common.utils.sms.ResultEnum;
import com.ruoyi.fangyuanapi.conf.TokenConf;
import com.ruoyi.fangyuanapi.conf.WxSmallConf;
import com.ruoyi.fangyuanapi.service.*;
import com.ruoyi.system.domain.DbUser;
import com.ruoyi.fangyuanapi.dto.DynamicDto;
import com.ruoyi.system.domain.DbUserLogin;
import com.ruoyi.system.feign.RemoteOssService;
import com.ruoyi.system.feign.SendSmsClient;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import jdk.nashorn.internal.runtime.logging.Logger;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.hibernate.validator.internal.util.logging.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("wxUser")
@Logger
public class DbUserController extends BaseController {

//
//    @Autowired
//    private RemoteDeptService remoteDeptService;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private IDbUserService dbUserService;

    @Autowired
    private IDbGiveLikeService dbGiveLikeService;

    @Autowired
    private IDbUserAndDynamicService dbUserAndDynamicService;

    @Autowired
    private IDbUserDynamicService dbUserDynamicService;

    @Autowired
    private SendSmsClient sendSmsClient;

    @Autowired
    private TokenConf tokenConf;

    @Autowired
    private IDbUserLoginService dbUserLoginService;

    @Autowired
    private WxSmallConf wxSmallConf;

    @Autowired
    private RemoteOssService remoteOssService;


    @PostMapping("filesUpload")
    @CrossOrigin
    public R avatarUpload(@RequestPart("files") MultipartFile[] files){
        if (files == null || files.length <= 0){
            return R.error("???????????????????????????????????????????????????");
        }
        ArrayList<String> list = new ArrayList<>();
        for (MultipartFile file : files) {
            long size = file.getSize();
            if (size > 1024*1024*2){
                logger.info(file.getOriginalFilename()+"???????????????????????????");
                return R.error("??????????????????2M?????????!");
            }
        }

        for (MultipartFile file : files) {
            if (file != null && StringUtils.checkFileIsImages(file.getOriginalFilename(),".jpg.png.jpeg.gif")){
                System.out.println(file.getOriginalFilename());
                R r = remoteOssService.editSave(file);
                if ("200".equals(r.get("code")+"")){
                    list.add(r.get("msg")+"");
                }
            }else {
                return R.error("????????????,?????????????????????????????????");
            }
        }
        return R.data(list);
    }

    @PostMapping("avatarUpload")
    @CrossOrigin
    public R avatarUpload(@RequestPart("file") MultipartFile file){
        if (file == null){
            return R.error("???????????????????????????????????????????????????");
        }

        if (StringUtils.checkFileIsImages(file.getOriginalFilename(),".jpg.png.jpeg.gif")){
            System.out.println(file.getOriginalFilename());
            R r = remoteOssService.editSave(file);
            if ("200".equals(r.get("code")+"")){
                return R.data(r.get("msg")+"");
            }
        }
        return R.error("????????????,?????????????????????????????????");
    }

    /**
     * ????????????
     *
     * @return
     */
    @GetMapping("exitLogin")
    @ApiOperation(value = "?????????????????????",notes = "????????????",httpMethod = "GET")
    public R exitLogin() {
        String userId = getRequest().getHeader(Constants.CURRENT_ID);
//        DbUserLogin login = dbUserLoginService.selectDbUserLoginByUserId(Long.valueOf(userId));
//        if (login == null){
//            return null;
//        }
//        /* ?????????????????? */
//        login.setStatus(1);
//        int i = dbUserLoginService.updateDbUserLogin(login);
        int i = 1;
        redisUtils.delete(RedisKeyConf.REFRESH_TOKEN_.name() + userId);
        return i > 0 ? R.data("?????????????????????") : R.error(ResultEnum.SERVICE_BUSY.getCode(), ResultEnum.SERVICE_BUSY.getMessage());
    }

    /**
     * ????????????
     *
     * @param phone
     * @param code
     * @param password
     * @return
     */
    @PutMapping("appUpdatePassword")
    @ApiOperation(value = "??????????????????",notes = "????????????",httpMethod = "PUT")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "phone",value = "?????????",required = true),
            @ApiImplicitParam(name = "code",value = "?????????",required = true),
            @ApiImplicitParam(name = "password",value = "??????",required = true),
            @ApiImplicitParam(name = "passwordAgain",value = "????????????",required = true)
    })
    public R appUpdatePassword(String phone, String code, String password,String passwordAgain) {
        if (StringUtils.isNotEmpty(phone) && PhoneUtils.checkPhone(phone) && StringUtils.isNotEmpty(code) && StringUtils.isNotEmpty(password) && StringUtils.isNotEmpty(passwordAgain)) {
            if (!StringUtils.checkPassword(password)){
                return R.error(ResultEnum.PASSWORD_NOT_RULE.getCode(),ResultEnum.PASSWORD_NOT_RULE.getMessage());
            }
            if (!password.equals(passwordAgain)){
                return R.error();
            }
            R r = sendSmsClient.checkCode(phone, code);
            if ("200".equals(r.get("code"))){
                String uuid = StringUtils.getUUID();
                String s = ZhaoMD5Utils.string2MD5(password + uuid);
                int i =  dbUserService.updateUserPassword(phone,s,uuid);
                if (i>0){
                    DbUser user = new DbUser();
                    user.setPhone(phone);
                    dbUserService.selectDbUserByPhone(user);
                    redisUtils.delete(RedisKeyConf.APP_ACCESS_TOKEN_.name() + user.getId());
                    return  R.ok("??????????????????,???????????????!");
                }
                return R.error();
            }
            return r;
        }
        return R.error(ResultEnum.PARAMETERS_ERROR.getCode(), ResultEnum.PARAMETERS_ERROR.getMessage());
    }


    /**
     * app????????????
     *
     * @param phone    ?????????
     * @param password ??????
     * @param code     ?????????
     * @return ????????????
     */
    @PostMapping("appLogin")
    @ApiOperation(value = "APP??????????????????",notes = "?????????code ?????????",httpMethod = "POST")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "phone",value = "?????????",required = true),
            @ApiImplicitParam(name = "password",value = "??????",required = false),
            @ApiImplicitParam(name = "code",value = "?????????",required = false)
    })
    public R appLogin(String phone, String password, String code) {
        DbUser dbUser = null;
        String s = redisUtils.get(CategoryType.PHONE_LOGIN_NUM_.name() + phone);
        Integer num = s == null ? 0 : Integer.valueOf(s);
        if (5 < Integer.valueOf(s == null ? "0" : s)) {//????????????
            return R.error(ResultEnum.LOGIN_HOUR_ERROR.getCode(), ResultEnum.LOGIN_HOUR_ERROR.getMessage());
        }
        if (phone != null && PhoneUtils.checkPhone(phone)) {
            DbUser user = new DbUser();
            user.setPhone(phone);
            dbUser = dbUserService.selectDbUserByPhone(user);
            redisUtils.set(CategoryType.PHONE_LOGIN_NUM_.name() + phone, 1, RedisTimeConf.ONE_HOUR);
            if (dbUser == null) {
                return R.error(ResultEnum.PHONE_NOT_REGISTER.getCode(), ResultEnum.PHONE_NOT_REGISTER.getMessage());
            }
        } else {
            return R.error(ResultEnum.PHONE_ERROR.getCode(), ResultEnum.PHONE_ERROR.getMessage());
        }
        String token = redisUtils.get(RedisKeyConf.REFRESH_TOKEN_.name() + dbUser.getId());

        if (dbUser != null && StringUtils.isNotEmpty(password) && StringUtils.isEmpty(code)) {
            //????????????
            String salt = dbUser.getSalt();
            if (StringUtils.isEmpty(dbUser.getPassword())){
                return R.error(ResultEnum.PASSWORD_IS_NULL.getCode(),ResultEnum.PASSWORD_IS_NULL.getMessage());
            }
            if (ZhaoMD5Utils.string2MD5(password + salt).equals(dbUser.getPassword())) {
                if (StringUtils.isNotEmpty(token)){
                    redisUtils.delete(RedisKeyConf.APP_ACCESS_TOKEN_.name() + dbUser.getId());
                }
                //????????????
                token = getToken(dbUser.getId(), tokenConf.getAccessTokenKey(), System.currentTimeMillis() + 1000L*60L*60L*24L*365L*3L,1);
                /* ??????token ???????????? */
                redisUtils.set(RedisKeyConf.APP_ACCESS_TOKEN_.name() + dbUser.getId(), token,60 * 60 * 24 * 365*3);
                return R.data(token);
            } else {
                redisUtils.set(CategoryType.PHONE_LOGIN_NUM_.name() + phone, num + 1, RedisTimeConf.ONE_HOUR);
                return R.error(ResultEnum.PASSWORD_ERROE.getCode(), ResultEnum.PASSWORD_ERROE.getMessage());
            }
        }

        if (dbUser != null && StringUtils.isEmpty(password) && StringUtils.isNotEmpty(code)) {//???????????????
            R r = sendSmsClient.checkCode(phone, code);
            if ("200".equals(r.get("code") + "")) {
                if (StringUtils.isNotEmpty(token)){
                    redisUtils.delete(RedisKeyConf.APP_ACCESS_TOKEN_.name() + dbUser.getId());
                }
                token = getToken(dbUser.getId(), tokenConf.getAccessTokenKey(),  System.currentTimeMillis() + 1000L*60L*60L*24L*365L*3L,1);
                redisUtils.set(RedisKeyConf.APP_ACCESS_TOKEN_.name() + dbUser.getId(),token,60 * 60 * 24 * 365*3);
                return R.data(token);
            } else {
                redisUtils.set(CategoryType.PHONE_LOGIN_NUM_.name() + phone, num + 1, RedisTimeConf.ONE_HOUR);
                return r;
            }
        }

        return R.error(ResultEnum.PARAMETERS_ERROR.getCode(), ResultEnum.PARAMETERS_ERROR.getMessage());
    }

    /**
     * @return
     */
    @PostMapping("refreshToken")
    @ApiOperation(value = "??????token??????",notes = "??????token",httpMethod = "POST")
    public R refreshToken() {
        String accessToken = getRequest().getHeader(Constants.TOKEN);
        if (StringUtils.isEmpty(accessToken)) {
            return null;
        }
        Map<String, Object> map = TokenUtils.verifyToken(accessToken, tokenConf.getRefreshTokenKey());
        if (map == null) {
            return R.error(ResultEnum.REFRESH_TOKEN_LOSE.getCode(), ResultEnum.REFRESH_TOKEN_LOSE.getMessage());
        }
        String id = map.get("id") + "";
        Map<String, String> tokens = resultTokens(Long.valueOf(id), "XIAOSI", tokenConf.getAccessTokenKey(), tokenConf.getRefreshTokenKey());
        redisUtils.set(RedisKeyConf.REFRESH_TOKEN_.name() + id, map.get("refreshToken"), 60 * 60 * 24 * 30 * 2L);
        return R.data(tokens);
    }


    /**
     * app??????
     *
     * @param phone         ?????????
     * @param password      ??????
     * @param passwordAgain ????????????
     * @param code          ?????????
     * @return token
     */
    @PostMapping("appRegister")
    @ApiOperation(value = "APP????????????",notes = "APP??????",httpMethod = "POST")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "phone",value = "?????????",required = true),
            @ApiImplicitParam(name = "password",value = "??????",required = true),
            @ApiImplicitParam(name = "passwordAgain",value = "????????????",required = true),
            @ApiImplicitParam(name = "code",value = "?????????",required = true),
    })
    public R appRegister(String phone, String password, String passwordAgain, String code) {
        if (StringUtils.isEmpty(phone) || StringUtils.isEmpty(password) || StringUtils.isEmpty(passwordAgain) || StringUtils.isEmpty(code)) {
            return R.error(ResultEnum.PARAMETERS_ERROR.getCode(), ResultEnum.PARAMETERS_ERROR.getMessage());
        }
        if (!password.equals(passwordAgain)) {//????????????
            return R.error();
        }
        if (!StringUtils.checkPassword(password)) {
            return R.error(ResultEnum.PASSWORD_NOT_RULE.getCode(), ResultEnum.PASSWORD_NOT_RULE.getMessage());
        }
        R r = sendSmsClient.checkCode(phone, code);
        if (!"200".equals(r.get("code") + "")) {
            return r;
        }
        DbUser user = new DbUser();
        user.setPhone(phone);
        DbUser dbUser = dbUserService.selectDbUserByPhone(user);

        String uuid = StringUtils.getUUID();
        String s = ZhaoMD5Utils.string2MD5(password + uuid);
        if (dbUser == null) {
            //???????????????????????????????????????????????????
            user.setSalt(uuid);
            user.setPassword(s);
            user.setCreateTime(new Date());
            user.setUserFrom(1);
            user.setPhoneIsVerify(0);
            user.setNickname(phone);
            int i = dbUserService.insertDbUser(user);
            return i > 0 ? R.data("????????????????????????????????????") : R.error(ResultEnum.SERVICE_BUSY.getCode(), ResultEnum.SERVICE_BUSY.getMessage());
        } else {
            if (dbUser.getUserFrom() == 1) {
                return R.error("??????????????????????????????");
            }
            if (dbUser.getUserFrom() == 0) {
                dbUser.setUpdateTime(new Date());//????????????
                dbUser.setPassword(s);
                dbUser.setSalt(uuid);
                int i = dbUserService.updateDbUser(dbUser);
                return i > 0 ? new R() : R.error(ResultEnum.SERVICE_BUSY.getCode(), ResultEnum.SERVICE_BUSY.getMessage());
            }
        }
        return R.error(ResultEnum.SERVICE_BUSY.getCode(), ResultEnum.SERVICE_BUSY.getMessage());
    }

    /**
     * ???????????????
     *
     * @param phone
     * @return R { 0 : SUCCESS}
     */
    @PostMapping("smallRegister")
    @ApiOperation(value = "???????????????",notes = "???????????????",httpMethod = "POST")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "phone",value = "?????????",required = true),
            @ApiImplicitParam(name = "code",value = "?????????",required = true),
            @ApiImplicitParam(name = "openId",value = "openId",required = true),
            @ApiImplicitParam(name = "nickname",value = "??????",required = false),
            @ApiImplicitParam(name = "avatar",value = "??????",required = false)
    })
    public R wxRegister(String phone,String code,String openId,String nickname,String avatar) {
        R r = sendSmsClient.checkCode(phone, code);
        //R r = new R();
        if ("200".equals(r.get("code")+"") ){
            DbUser user = new DbUser();
            user.setPhone(phone);
            DbUser dbUser= dbUserService.selectDbUserByPhone(user);
            if (dbUser == null ){
                dbUser = dbUserService.wxRegister(phone,openId,nickname,avatar);
                return R.data(getToken(dbUser.getId(), tokenConf.getAccessTokenKey(),System.currentTimeMillis() + 1000L*60L*60L*24L*365L*3L,0));
            }
            if (dbUser != null && StringUtils.isEmpty(dbUser.getOpenId())){//????????????
                dbUser.setOpenId(openId);
                if (dbUser.getAvatar() == null){
                    dbUser.setAvatar(avatar);
                }
                if (dbUser.getNickname() == null){
                    dbUser.setNickname(nickname);
                }
                dbUserService.updateDbUser(dbUser);
                return R.data(getToken(dbUser.getId(), tokenConf.getAccessTokenKey(),System.currentTimeMillis() + 1000L*60L*60L*24L*365L*3L,0));
            }
            return R.error(ResultEnum.PARAMETERS_ERROR.getCode(),ResultEnum.PARAMETERS_ERROR.getMessage());
        }else {
            return r;
        }
    }


    /**
     * ??????openId
     * @param code
     * @return
     */
    @PostMapping("getOpenId")
    @ApiOperation(value = "??????openId??????",notes = "????????????code???????????????openid???????????????????????????token",httpMethod = "POST")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "code", value = "??????????????????????????????code?????????????????????", required = true)
    })
    public R getOpenId(String code){
        try {
            HashMap<String, String> map = new HashMap<>();
            map.put("appid",wxSmallConf.getAppid());
            map.put("secret",wxSmallConf.getSecret());
            map.put("js_code",code);
            map.put("grant_type",wxSmallConf.getGrant_type());
            HttpResponse response = HttpUtil.doGet(wxSmallConf.getHost(), wxSmallConf.getPath(), HttpMethod.GET.name(), null, map);
            System.out.println(response.toString());
            String string = EntityUtils.toString(response.getEntity());
            Map result = JSONUtil.toBean(string, Map.class);
            String openid;
            try {
                openid = result.get("openid").toString();
            }catch (Exception e){
                openid =null;
            }
            Map<String, String> stringMap = new HashMap<>();
            if (StringUtils.isNotEmpty(openid)){
                DbUser user = dbUserService.selectDbUserByOpenId(openid);
                if (user != null){
                    stringMap.put("token",getToken(user.getId(), tokenConf.getAccessTokenKey(),System.currentTimeMillis() + 1000L*60L*60L*24L*365L*3L,0));
                    return  R.data(stringMap) ;
                }
                stringMap.put("openId",openid);
                return  R.data(stringMap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return R.error();
    }

    private String getToken(Long id, String key, Long time, Integer type){
        HashMap<Object, Object> tokenMap = new HashMap<>();
        tokenMap.put("id", id);
        tokenMap.put("expireTime", time);
        tokenMap.put("topic", 0);
        if (type != null){
            tokenMap.put("type",type);
        }
        return TokenUtils.encrypt(JSON.toJSONString(tokenMap), key);
    }

    /**
     * ??????????????????????????????????????????????????????????????????????????????????????????
     *
     * @return
     */
    @GetMapping("userInfo")
    @ApiOperation(value = "??????????????????",notes = "????????????",httpMethod = "GET")
    public R getUserInfo() {
        String userId = getRequest().getHeader(Constants.CURRENT_ID);
        DbUser dbUser = dbUserService.selectDbUserById(Long.valueOf(userId));
        HashMap<String, Object> map = new HashMap<>();
        map.put("nickname",dbUser.getNickname());
        map.put("avatar",dbUser.getAvatar());
        map.put("dynamicNum",dbUser.getDynamicNum() );
        map.put("attentionNum",dbUser.getAttentionNum());
        map.put("replyAttentionUserNum",dbUser.getReplyAttentionUserNum());
        return R.data(map);
    }

    /**
     * ????????????
     *
     * @return
     */
    @GetMapping("dynamic/{currPage }")
    @ApiOperation(value = "??????????????????",notes = "????????????",httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "currPage",value = "???????????????:1",required = true)
    })
    public R getUserDynamic(@PathVariable Integer currPage) {
        String userId = getRequest().getHeader(Constants.CURRENT_ID);
        DbUser dbUser = dbUserService.selectDbUserById(Long.valueOf(userId));
        if (dbUser == null) {
            //????????????????????????????????????????????????????????????????????????
            return null;
        }
        currPage = currPage < 0 ? 0:(currPage -1) * PageConf.pageSize;
        List<DynamicDto> dynamic = dbUserService.getUserDynamic(dbUser, currPage, PageConf.pageSize);
        return R.data(dynamic);
    }

    /**
     * ?????????
     *
     * @return
     */
    @GetMapping("giveLikeNum")
    @ApiOperation(value = "???????????????",notes = "?????????",httpMethod = "GET")
    public R getGiveLikeSum() {
        String userId = getRequest().getHeader(Constants.CURRENT_ID);
        Integer likeNum = dbGiveLikeService.selectUserGiveLikeNum(userId);
        return likeNum != null ? new R() : R.error();
    }

    /**
     * ????????????
     *
     * @param currPage
     * @return list<string>
     */
    @GetMapping("photoAlbum/{currPage}")
    @ApiOperation(value = "????????????",notes = "????????????",httpMethod = "GET")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "currPage",value = "???????????????:1",required = true)
    })
    public R getPhotoAlbum(@PathVariable Integer currPage) {
        String userId = getRequest().getHeader(Constants.CURRENT_ID);
        currPage = currPage < 0 ? 0:(currPage -1) * PageConf.pageSize;
        List<Long> dynamicIds = dbUserAndDynamicService.selectDbUserAndDynamicByUserId(Long.valueOf(userId));
        if (dynamicIds == null || dynamicIds.size() <= 0) {
            return R.ok("?????????????????????");
        }
        List<Map<String, String>> PhotoAlbum = dbUserDynamicService.selectImagesByDynamicId(dynamicIds, currPage,PageConf.pageSize);
        return R.data(PhotoAlbum);
    }

    /**
     * ????????????????????????
     *
     * @param request
     * @return
     */
    @GetMapping("getUserData")
    @ApiOperation(value = "??????????????????",notes = "??????????????????",httpMethod = "GET")
    public R getUserDate(HttpServletRequest request) {
        String userId = request.getHeader(Constants.CURRENT_ID);
        Map<String, String> map = dbUserService.getUserData(Long.valueOf(userId));
        return map == null ? R.error() : R.data(map);
    }

    /**
     * ??????????????????
     *
     * @return
     */
    @PutMapping("UpdateUserData")
    @ApiOperation(value = "??????????????????",notes = "????????????????????????", httpMethod = "PUT")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "avatar",value = "??????",required = true),
            @ApiImplicitParam(name = "username",value = "????????????",required = true),
            @ApiImplicitParam(name = "nickName",value = "??????",required = true),
            @ApiImplicitParam(name = "gender",value = "??????",required = true),
            @ApiImplicitParam(name = "birthday",value = "??????",required = true),
            @ApiImplicitParam(name = "signature",value = "?????? ??????????????????",required = true),
    })
    public R UpdateUserData(String avatar,String username,String nickName,Integer gender,Date birthday,String signature) {
        String userId = getRequest().getHeader(Constants.CURRENT_ID);
        DbUser dbUser = new DbUser();
        dbUser.setUserName(username);
        dbUser.setNickname(nickName);
        dbUser.setGender(gender);
        dbUser.setBirthday(birthday);
        dbUser.setSignature(signature);
        dbUser.setAvatar(avatar);
        dbUser.setId(Long.valueOf(userId));
        int i = dbUserService.updateDbUser(dbUser);
        return i > 0 ? new R() : R.error(ResultEnum.SERVICE_BUSY.getCode(), ResultEnum.SERVICE_BUSY.getMessage());
    }

    private void insertLoginStatus(DbUserLogin login, Long id, Integer userFrom) {
        HttpServletRequest request = getRequest();
        if (login != null) {
            login.setStatus(0);
            int i = dbUserLoginService.updateDbUserLogin(login);
            return;
        }
        login = new DbUserLogin();
        login.setLoginId(id);
        login.setLoginFrom(userFrom);
        login.setLoginTime(new Date());
        login.setLocationInfo(IpUtils.getIpAddr(request));
        dbUserLoginService.insertDbUserLogin(login);
    }

    private Map<String, String> resultTokens(Long id, String publisher, String accessTokenKey, String refreshTokenKey) {
        HashMap<String, String> map = new HashMap<>();
        Long expireTime = System.currentTimeMillis() + (1000 * 60 * 60 * 24 * 60);
        String accessToken = TokenUtils.getToken(id, expireTime, publisher, "accessToken", accessTokenKey);
        map.put("accessToken", accessToken);
        Long refreshTime = System.currentTimeMillis() + (1000 * 60 * 60 * 30);
        String refreshToken = TokenUtils.getToken(id, refreshTime, publisher, "refreshToken", refreshTokenKey);
        map.put("refreshToken", refreshToken);
        return map;
    }



    /*
    *
    * ????????????
    * */
    @GetMapping("getUserList")
    @ApiOperation(value = "??????????????????",notes = "??????????????????",httpMethod = "GET")
    public R getUserList() {
        DbUser dbUser = new DbUser();
        List<DbUser> dbUsers = dbUserService.selectDbUserList(dbUser);
        return R.data(dbUsers);
    }


}

