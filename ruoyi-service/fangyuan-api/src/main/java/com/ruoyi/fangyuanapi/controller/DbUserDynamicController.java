package com.ruoyi.fangyuanapi.controller;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.qiniu.common.QiniuException;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.page.PageConf;
import com.ruoyi.common.json.JSONUtils;
import com.ruoyi.common.redis.config.RedisKeyConf;
import com.ruoyi.common.redis.config.RedisTimeConf;
import com.ruoyi.common.redis.util.KeyUtils;
import com.ruoyi.common.redis.util.RedisUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.md5.ZhaoMD5Utils;
import com.ruoyi.common.utils.sensitivewdfilter.WordFilter;
import com.ruoyi.common.utils.sms.CategoryType;
import com.ruoyi.common.utils.sms.ResultEnum;
import com.ruoyi.fangyuanapi.conf.QiniuConf;
import com.ruoyi.fangyuanapi.conf.QiniuUtils;
import com.ruoyi.fangyuanapi.dto.DynamicDto;
import com.ruoyi.fangyuanapi.dto.UserDto;
import com.ruoyi.fangyuanapi.service.*;
import com.ruoyi.system.domain.*;
import com.ruoyi.system.feign.RemoteOssService;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.fangyuanapi.service.IDbUserDynamicService;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 动态 提供者
 *
 * @author fangyuan
 * @date 2020-09-07
 */

@RestController
@Api("动态")
@RequestMapping("dynamic1")
public class DbUserDynamicController extends BaseController {

	@Autowired
	private IDbUserDynamicService dbUserDynamicService;

	@Autowired
	private QiniuUtils qiniuUtils;

	@Autowired
	private QiniuConf qiniuConf;

	@Autowired
	private RemoteOssService remoteOssService;

	@Autowired
	private RedisUtils redisUtils;

	@Autowired
	private JSONUtils utils;

	@Autowired
	private IDbManualReviewService dbManualReviewService;

	@Autowired
	private IDbUserAndDynamicService dbUserAndDynamicService;

	@Autowired
	private IDbAttentionService dbAttentionService;

	@Autowired
	private IDbGiveLikeService dbGiveLikeService;

	@Autowired
	private IDbUserService dbUserService;

	@Autowired
	private ListOperations listOperations;
	@Autowired
	private HashOperations hashOperations;

	@Autowired
	private IDbDynamicAndEntryService dbDynamicAndEntryService;

	@Autowired
	private IDbEntryService dbEntryService;

	@Autowired
	private IDbForwardService dbForwardService;

	@GetMapping("searchDynamic")
	@ApiOperation(value = "动态搜索接口",notes = "关键字搜索",httpMethod = "GET")
	public R searchDynamic( String  word){
		if (StringUtils.isEmpty(word)){
			return R.error(ResultEnum.PARAMETERS_ERROR.getCode(),ResultEnum.PARAMETERS_ERROR.getMessage());
		}
		List<DbUserDynamic> dynamics = dbUserDynamicService.searchDynamic(word);
		String userId = getRequest().getHeader(Constants.CURRENT_ID);
		List<Long> ids = null;
		if (StringUtils.isNotEmpty(userId)){
			ids = dbAttentionService.selectReplyAttentionUserIds(userId);
		}
		ArrayList<DynamicDto> dtos = new ArrayList<>();
		for (DbUserDynamic dynamic : dynamics) {
			DynamicDto dto = getDynamicDto(dynamic, ids);
			dtos.add(dto);
		}
		return dtos.size()>0?R.data(dtos):R.ok("没有搜索结果！");
	}

	/**
	 * 获取动态
	 * @return
	 */
	@GetMapping("getDynamic/{currPage}")
	@ApiOperation(value = "获取动态接口",notes = "获取动态",httpMethod = "GET")
	@ApiImplicitParams(
			@ApiImplicitParam(name = "currPage",value = "当前页")
	)
	public R getDynamic(@PathVariable(value = "currPage") Integer currPage){
		currPage = currPage <= 0 ? 0 :(currPage -1) * PageConf.pageSize;
		Set<String> set = redisUtils.revRange(RedisKeyConf.DYNAMIC_ORDER.name(), currPage, PageConf.pageSize);
		String falg = redisUtils.get(RedisKeyConf.INSERT_FLAG.name());
		//先查缓存
		ArrayList<DynamicDto> list = new ArrayList<>();
		String userId = getRequest().getHeader(Constants.CURRENT_ID);
		List<Long> attentionIds = null;
		if (StringUtils.isNotEmpty(userId)){
			attentionIds = dbAttentionService.selectDbAttentionByUserId(userId);
		}
		if(set != null && set.size()>0 && currPage < 200 && StringUtils.isEmpty(falg) ){

			for (String e : set) {
				Object o = hashOperations.get(RedisKeyConf.DYNAMIC_ARRAY_.name(), e);
				DynamicDto dto = JSON.parseObject(o.toString(), DynamicDto.class);
				UserDto user = dto.getUser();
				user.setIsAttention(getIsAttention(attentionIds,user.getUserId()));
				dto.setUser(user);
				list.add(dto);
			}
			return R.data(list);
		}else {
			//没有缓存
			List<DynamicDto> result = getDynamicListFromDb(currPage,PageConf.pageSize,attentionIds);
			return result.size()>0 ? R.data(result):R.error(ResultEnum.DYNAMIC_IS_NULL.getCode(),ResultEnum.DYNAMIC_IS_NULL.getMessage());
		}
	}


	/**
	 * 用户给动态点赞接口
	 * @param dynamicId
	 * @return
	 */
	@GetMapping("like/{dynamicId}")
	@ApiOperation(value = "获取动态接口",notes = "获取动态")
	@ApiImplicitParams(
			@ApiImplicitParam(name = "currPage",value = "当前页")
	)
	public R like(@PathVariable(name = "dynamicId",required = true) Long dynamicId){
		String userId = getRequest().getHeader(Constants.CURRENT_ID);
		DbUserDynamic dbUserDynamic = dbUserDynamicService.selectDbUserDynamicById(dynamicId);
		if (dbUserDynamic == null){
			return null;
		}else {
			boolean b = dbGiveLikeService.insertDbGiveLikeAndLikeNum(Long.valueOf(userId),dynamicId);
			return null;
		}
	}

	/**
	 * 获取关注的朋友的动态
	 * @param currPage
	 * @return
	 */
	@RequestMapping("getAttentionUserDynamic")
	@ApiOperation(value = "获取动态接口",notes = "获取动态")
	@ApiImplicitParams(
			@ApiImplicitParam(name = "currPage",value = "当前页")
	)
	public R getAttentionUserDynamic( Integer currPage) {
		String userId = getRequest().getHeader(Constants.CURRENT_ID);
		currPage = currPage == null || currPage <= 0 ? 0 : (currPage - 1) * PageConf.pageSize;
		List<Long> ids = dbAttentionService.selectReplyAttentionUserIds(userId);//关注的userIdS
		if (ids == null) { //没有关注的人
			return R.error(ResultEnum.NULL_ATTENTION.getCode(), ResultEnum.NULL_ATTENTION.getMessage());
		}
		ArrayList<DbUserDynamic> dynamics = new ArrayList<>();
		ids.forEach(e -> {//获取关注的人的动态,
			List<Long> list = dbUserAndDynamicService.selectDbUserAndDynamicByUserId(Long.valueOf(userId));//动态id
			list.forEach(d -> {
				DbUserDynamic dynamic = dbUserDynamicService.selectDbUserDynamicByIdAndPermission(d);
				dynamics.add(dynamic);
				redisUtils.zSetAdd(RedisKeyConf.REDIS_ZSET_.name() + userId, String.valueOf(dynamic.getId()), dynamic.getCreatedTime().getTime());
			});
		});

		ArrayList<DbUserDynamic> result = new ArrayList<>();
		Set<String> setIds = redisUtils.revRange(RedisKeyConf.REDIS_ZSET_.name() + userId, currPage, PageConf.pageSize);
		if (dynamics.size() <= 10) {
			return R.data(dynamics);
		}
		//取10条
		dynamics.forEach(a -> {
			setIds.forEach(b -> {
				if (a.getId().equals(b)) {
					result.add(a);
				}
			});
		});
		redisUtils.delete(RedisKeyConf.REDIS_ZSET_ + userId);

		return R.data(result);
	}


	/**
	 * @param text      动态发布的内容
	 * @param file      资源数组： 图片可有六个 视频一个
	 * @param authority 权限：谁可见
	 * @param entryIds  词条数组
	 * @param site      发表动态时的位置
	 * @return
	 *//*
	 * @return jobId或者人工审核Id
	 */
	@PostMapping(value = "insertDynamic", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ApiOperation(value = "新增动态接口", notes= "新增动态",httpMethod = "POST")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "text",value = "评论内容",required = true),
			@ApiImplicitParam(name = "file",value = "动态资源数组(图片/视屏)",required = true),
			@ApiImplicitParam(name = "text",value = "评论内容",required = true),
			@ApiImplicitParam(name = "text",value = "评论内容",required = true),
	})
	public R insertDynamic( @RequestParam("text") String text, @RequestPart("file") MultipartFile[] file, @RequestParam(value = "authority", required = false) Integer authority, @RequestParam(value = "entryIds", required = false) Long[] entryIds, @RequestParam(value = "site", required = false) String site) {
		String userId = getRequest().getHeader(Constants.CURRENT_ID);
		DbUserDynamic dynamic = null;
		if (StringUtils.isNotEmpty(text) && file != null && file.length > 0 && file.length <= 6) {
			if (WordFilter.isContains(text)) {
				return R.error(ResultEnum.TEXT_ILLEGAL.getCode(), ResultEnum.TEXT_ILLEGAL.getMessage());
			}
			boolean isReview = false;
			dynamic = new DbUserDynamic();//内容合法初始化动态对象
			dynamic.setContent(text);//内容
			dynamic.setPermission(authority);//权限
			dynamic.setOrientation(site);//位置
			ArrayList<String> urls = new ArrayList<>();//装多个url
			for (MultipartFile multipartFile : file) {
				if (StringUtils.checkFileIsImages(multipartFile.getOriginalFilename(), qiniuConf.getImageFilter())) {//是图片调用上传接口
					//String url = dbUserDynamicService.uploadFile(multipartFile);
					dynamic.setIsHaveVoide(1);//没有视频
					R r = remoteOssService.editSave(multipartFile);
					String url = (String) r.get("msg");//上传
					if (StringUtils.isEmpty(url)) {
						return R.error(ResultEnum.SERVICE_BUSY.getCode(), ResultEnum.SERVICE_BUSY.getMessage());
					}
					String s = qiniuUtils.checkImage(url);
					switch (s) {
						case "block":
							//不合法直接返回
							return R.error(ResultEnum.RESULT_BLOCK.getCode(), ResultEnum.RESULT_BLOCK.getMessage());
						case "review":
							//人工审核
							dynamic.setIsBanned(1);
							isReview = true;
							break;
						default:
					}
					urls.add(url);
				}
				if (StringUtils.checkFileIsVideo(multipartFile.getOriginalFilename(), qiniuConf.getVideoUrl())) {//是视频立刻返回接果
					String videoUrl = dbUserDynamicService.uploadFile(multipartFile);
					if (videoUrl == null) {
						return R.error(ResultEnum.SERVICE_BUSY.getCode(), ResultEnum.SERVICE_BUSY.getMessage());
					}
					urls.add(videoUrl);
					dynamic.setIsHaveVoide(0);//有视频
					dynamic.setResource(utils.listToJsonArray(urls));
					dynamic.setIsBanned(1);
					String jobId = qiniuUtils.videoCheck(videoUrl);
					redisUtils.set(jobId, dynamicDataToString(userId, dynamic, entryIds), RedisTimeConf.THREE_DAY);//存放审核完成后要插入的数据
					return R.data(jobId);
				}
			}
			String urlString = utils.listToJsonArray(urls);
			dynamic.setResource(urlString);
			if (isReview) {//插入人工审核表
				DbManualReview manualReview = new DbManualReview();
				manualReview.setUserId(Long.valueOf(userId));
				manualReview.setDynamicContent(text);
				manualReview.setDynamicResource(urlString);
				manualReview.setCreated(new Date());
				int i = dbManualReviewService.insertDbManualReview(manualReview);
				redisUtils.set(manualReview.getId() + "", dynamicDataToString(userId, dynamic, entryIds), RedisTimeConf.THREE_DAY);//存放审核完成后要插入的数据
				return R.data(ZhaoMD5Utils.string2MD5(manualReview.getId() + ""));//人工审核id
			}
			DbUserDynamic dynamic1 = dbUserDynamicService.insterDynamic(userId, dynamic, entryIds);
			//PASS直接插入数据
			/* 开启缓存之后 每次新增数据插入缓存 */
			if (redisUtils.getKeyIsExist(RedisKeyConf.DYNAMIC_ORDER.name())) {
				setDynamicArray(getDynamicDto(dynamic1,null));
				return R.ok();
			}
		}
		return R.error(ResultEnum.PARAMETERS_ERROR.getCode(), ResultEnum.PARAMETERS_ERROR.getMessage());
	}

	/**
	 * 视频审核回调接口
	 *
	 * @param request
	 * @param jodId
	 * @return
	 */
	@GetMapping("videoCallback/{jobId}")
	public R videoCallback(HttpServletRequest request, @PathVariable String jodId) {
		String userId = request.getHeader(Constants.CURRENT_ID);
		String dynamic = redisUtils.get(jodId);
		if (StringUtils.isEmpty(dynamic)) {
			return R.error(ResultEnum.PARAMETERS_ERROR.getCode(), ResultEnum.PARAMETERS_ERROR.getMessage());
		}
		try {
			String result = qiniuUtils.getCheckVideoResult(jodId);
			Map<String, String> map = utils.stringToMap(dynamic);
			DbUserDynamic dynamic1 = JSON.parseObject(map.get("dynamic"), DbUserDynamic.class);
			Long[] entryIds = JSON.parseObject(map.get("entryIds"), Long[].class);
			switch (result) {
				case "block":
					//违规
					dbUserDynamicService.deleteDbUserDynamicById(Long.valueOf(dynamic));
					return R.error("该视频违规！");
				case "review":
					//需人工审核
					DbManualReview manualReview = new DbManualReview();
					manualReview.setUserId(Long.valueOf(userId));
					manualReview.setDynamicContent(dynamic1.getContent());
					manualReview.setDynamicResource(dynamic1.getResource());
					manualReview.setCreated(new Date());
					int i = dbManualReviewService.insertDbManualReview(manualReview);
					redisUtils.set(manualReview.getId() + "", dynamic, RedisTimeConf.THREE_DAY);//存放审核完成后要插入的数据
					//删除视频上传之后放的数据
					redisUtils.delete(jodId);
					return R.data(ZhaoMD5Utils.string2MD5(manualReview.getId() + ""));//人工审核id
				case "pass":
					//插入对象
					DbUserDynamic userDynamic = dbUserDynamicService.insterDynamic(userId, dynamic1, entryIds);
					//zset key   dynamicId  createdTime             list key  dynamic dynamicJson 1000 tiao
					/* 开启缓存之后 每次新增数据插入缓存 */
					if (redisUtils.getKeyIsExist(RedisKeyConf.DYNAMIC_ORDER.name())){
						setDynamicArray(getDynamicDto(userDynamic,null));
					}
					return userDynamic == null ? R.error(ResultEnum.SERVICE_BUSY.getCode(), ResultEnum.SERVICE_BUSY.getMessage()) : R.ok("审核已通过！");
				default:
					return R.error();
			}
		} catch (QiniuException e) {
			e.printStackTrace();
		}
		return R.error();
	}

	/**
	 * 人工审核结果查询
	 *
	 * @param reviewId 查询码 MD5摘要
	 * @return 结果
	 */
	@GetMapping("getReviewResult/{reviewId}")
	public R getReviewResult(HttpServletRequest request, @PathVariable String reviewId) {
		String userId = request.getHeader(Constants.CURRENT_ID);
		reviewId = ZhaoMD5Utils.convertMD5(reviewId);
		String data = redisUtils.get(reviewId);
		if (StringUtils.isEmpty(data)) {
			return null;//没有命中缓存，不处理
		}
		DbManualReview dbManualReview = dbManualReviewService.selectDbManualReviewById(Long.valueOf(reviewId));
		if (1 == dbManualReview.getIsSussess() || 0 == dbManualReview.getIsViolation()) {
			Map<String, String> map = utils.stringToMap(data);
			DbUserDynamic dynamic1 = JSON.parseObject(map.get("dynamic"), DbUserDynamic.class);
			Long[] entryIds = JSON.parseObject(map.get("entryIds"), Long[].class);
			DbUserDynamic dynamic = dbUserDynamicService.insterDynamic(userId, dynamic1, entryIds);
			/* 开启缓存之后 每次新增数据插入缓存 */
			if (redisUtils.getKeyIsExist(RedisKeyConf.DYNAMIC_ORDER.name())){
				setDynamicArray(getDynamicDto(dynamic,null));
			}
			return dynamic == null ? R.error(ResultEnum.SERVICE_BUSY.getCode(), ResultEnum.SERVICE_BUSY.getMessage()) : R.ok("审核已通过！");
		}
		if (1 == dbManualReview.getIsSussess() || 1 == dbManualReview.getIsViolation()) {
			redisUtils.delete(reviewId);//审核失败
			return R.error(ResultEnum.VIOLATION_FAILURE.getCode(), ResultEnum.VIOLATION_FAILURE.getMessage());
		}
		return R.error(ResultEnum.UNDER_REVIEW.getCode(), ResultEnum.UNDER_REVIEW.getMessage());
	}

	@GetMapping("startCache")
	public R insertCache(){
		ArrayList<DbUserDynamic> list = new ArrayList<>(2000);
		list = dbUserDynamicService.selectDynamicList(0,2000);
		if (list != null && list.size()>300 ){
			try {
				redisUtils.set(RedisKeyConf.INSERT_FLAG.name(),"1");
				list.stream().forEach(e -> setDynamicArray(getDynamicDto(e,null)));
				return R.ok("预热完成！");
			}catch (Exception e){

			}finally {
				redisUtils.delete(RedisKeyConf.INSERT_FLAG.name());
			}
		}
		return R.error();
	}

	private List<DynamicDto> getDynamicListFromDb(Integer currPage,Integer pageSize,List<Long> attentionIds){
		ArrayList<DbUserDynamic> dynamics = dbUserDynamicService.selectDbUserDynamicOrderByCreateTime(currPage,pageSize);
		ArrayList<DynamicDto> list = new ArrayList<>();
		for (DbUserDynamic e : dynamics) {
			DynamicDto dto = getDynamicDto(e,attentionIds);
			list.add(dto);
		}
		return list;
	}

	/**
	 * 是否关注
	 * @param attentionIds
	 * @param userId
	 * @return
	 */
	private Integer getIsAttention(List<Long> attentionIds,Long userId){
		if (attentionIds != null && attentionIds.size() > 0){
			for (Long id : attentionIds) {
				if (id == userId){
					return 1;
				}
			}
		}
		return 0;
	}

	private DynamicDto getDynamicDto(DbUserDynamic dynamic,List<Long> attentionIds){
		String header= getRequest().getHeader(Constants.CURRENT_ID);
		DynamicDto dto = new DynamicDto();
		DbUser dbUser = null;
		Date crate = null;
		UserDto userDto = new UserDto();
		if (dynamic.getIsForward() == 1){
			//转发的动态
			DbForward forward =  dbForwardService.selectDbForwardByDynamicId(dynamic.getId());
			dto.setForwardComment(forward.getForwardComment());
			dto.setForwardUserId(forward.getUserId());
			dbUser = dbUserService.selectDbUserById(forward.getUserId());
			/* 转发的时间 */
			crate = forward.getForwardTime();
			dto.setForwardNickName(dbUser.getNickname());
		}
		dto.setIsForward(dynamic.getIsForward());
		dto.setDynamicId(dynamic.getId());
		dto.setResource(JSON.parseArray(dynamic.getResource(),String.class));
		dto.setIsHaveVideo(dynamic.getIsHaveVoide());
		dto.setOrientation(dynamic.getOrientation());
		dto.setContent(dynamic.getContent());
		dto.setLiveGiveSum(Integer.valueOf(dynamic.getLikeNum()+""));
		/* 动态点赞数量不为0 并且用户处于登陆状态 查询这个动态该用户是否点赞 */
		Integer flag = 0;
		if (dynamic.getLikeNum() > 0 && StringUtils.isNotEmpty(header)){
			DbGiveLike giveLike = dbGiveLikeService.selectDbGiveLikeByUserIdAndDynamicId(header,dynamic.getId());
			flag = giveLike.getId() != null ? 1 : flag;
		}
		dto.setLikeFlag(flag);
		dto.setCommentSum(Integer.valueOf(dynamic.getCommentNum()+""));
		dto.setForwardSum(Integer.valueOf(dynamic.getForwardNum()+""));
		Long userId = dbUserAndDynamicService.selectDbUserAndDynamicByDynamicId(dynamic.getId());
		userDto.setIsAttention(getIsAttention(attentionIds,userId));
		userDto.setUserId(userId);
		dbUser = dbUserService.selectDbUserById(userId);
		if (dynamic.getIsForward() == 0){
			userDto.setAvatar(dbUser.getAvatar());
			crate = dynamic.getCreatedTime();
		}
		dto.setCreatedTime(crate);
		userDto.setNickname(dbUser.getNickname());
		DbDynamicAndEntry dynamicAndEntry = new DbDynamicAndEntry();
		dynamicAndEntry.setDynamicId(dynamic.getId());
		List<DbDynamicAndEntry> entries = dbDynamicAndEntryService.selectDbDynamicAndEntryList(dynamicAndEntry);
		/* todo 后续在修改*/
		if (entries != null && entries.size()>0){
			ArrayList<String> list = new ArrayList<>();
			entries.forEach(e ->{
				DbEntry dbEntry = dbEntryService.selectDbEntryById(e.getId());
				list.add(dbEntry.getEntryName());
			});
			dto.setRelSet(list);
		}
		dto.setUser(userDto);
		return dto;
	}

	/**
	 * 转换json字符
	 *
	 * @param userId
	 * @param dynamic
	 * @param entryIds
	 * @return
	 */
	private String dynamicDataToString(String userId, DbUserDynamic dynamic, Long[] entryIds) {
		HashMap<String, String> map = new HashMap<>();
		String s1 = utils.objectToString(dynamic);
		map.put("userId", userId);
		map.put("dynamic", s1);
		map.put("entryIds", utils.objectToString(entryIds));

		return utils.mapToString(map);
	}

	/**
	 * 维护redis列表
	 * @param userDynamic
	 */
	private void setDynamicArray(DynamicDto userDynamic){
		String dynamicJson = JSON.toJSONString(userDynamic);
		Long card = redisUtils.getZSetCard(RedisKeyConf.DYNAMIC_ORDER.name());
		if (card >2000){
			redisUtils.removeByRank(RedisKeyConf.DYNAMIC_ORDER.name(),0L,0L);
			Long value = redisUtils.getZSetValue(RedisKeyConf.DYNAMIC_ORDER.name());
			hashOperations.delete(RedisKeyConf.DYNAMIC_ARRAY_,value);
		}
		Boolean aBoolean = hashOperations.getOperations().hasKey("");
		redisUtils.zSetAdd(RedisKeyConf.DYNAMIC_ORDER.name(),userDynamic.getDynamicId()+"",Double.valueOf(userDynamic.getCreatedTime().getTime()+""));
		hashOperations.put(RedisKeyConf.DYNAMIC_ARRAY_.name(),userDynamic.getDynamicId()+"",dynamicJson);
		//修剪list容量
		//listOperations.trim(RedisKeyConf.DYNAMIC_ARRAY.name(),0L,10000L);
	}







	@GetMapping("get/{id}")
	@ApiOperation(value = "根据id查询" , notes = "查询${tableComment}")
	public DbUserDynamic get(@ApiParam(name="id",value="long",required=true)  @PathVariable("id") Long id)
	{
		return dbUserDynamicService.selectDbUserDynamicById(id);

	}


	@GetMapping("list")
	@ApiOperation(value = "查询动态列表" , notes = "动态列表")
	public R list(@ApiParam(name="DbUserDynamic",value="传入json格式",required=true) DbUserDynamic dbUserDynamic)
	{
		startPage();
		return result(dbUserDynamicService.selectDbUserDynamicList(dbUserDynamic));
	}



	@PostMapping("save")
	public R addSave( @RequestBody DbUserDynamic dbUserDynamic)
	{
		return toAjax(dbUserDynamicService.insertDbUserDynamic(dbUserDynamic));
	}


	@PostMapping("update")
	public R editSave( @RequestBody DbUserDynamic dbUserDynamic)
	{
		return toAjax(dbUserDynamicService.updateDbUserDynamic(dbUserDynamic));
	}


	@PostMapping("remove")
	public R remove( String ids)
	{
		return toAjax(dbUserDynamicService.deleteDbUserDynamicByIds(ids));
	}




}
