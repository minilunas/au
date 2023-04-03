package com.au.sa.fssc.claim.img.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.au.fssc.bpm.api.param.dto.callBpm.CallProcessRequestDto;
import com.au.fssc.bpm.api.param.dto.callBpm.CallProcessVariables;
import com.au.fssc.bpm.api.param.dto.callBpm.WorkflowSubmitAndAgreeResult;
import com.au.sa.fssc.bpm.api.BpmApiService;
import com.au.sa.fssc.claim.base.claim.common.service.IBaseClaimRelService;
import com.au.sa.fssc.claim.base.claim.common.service.IBaseClaimService;
import com.au.sa.fssc.claim.base.claim.constant.TProcessWiparticipantRecord;
import com.au.sa.fssc.claim.base.claim.head.entities.TRmbsClaim;
import com.au.sa.fssc.claim.base.claim.head.entities.TRmbsClaimLock;
import com.au.sa.fssc.claim.base.claim.head.params.ClaimPageParams;
import com.au.sa.fssc.claim.base.claim.head.repository.RmbsClaimRepository;
import com.au.sa.fssc.claim.base.claim.head.service.IBaseClaimDeleteService;
import com.au.sa.fssc.claim.base.claim.head.service.IBaseClaimProcessService;
import com.au.sa.fssc.claim.base.claim.head.service.ITRmbsClaimLockService;
import com.au.sa.fssc.claim.base.claim.line.entities.TRmbsClaimLine;
import com.au.sa.fssc.claim.base.claim.line.repository.RmbsClaimLineRepository;
import com.au.sa.fssc.claim.base.claim.process.entities.TProcessWiRecord;
import com.au.sa.fssc.claim.base.claim.process.repository.TProcessWiRecordRepository;
import com.au.sa.fssc.claim.base.claim.rel.params.ClaimRelPageParams;
import com.au.sa.fssc.claim.base.claim.rel.params.ClaimRelReqParams;
import com.au.sa.fssc.claim.base.claim.sys.entities.TRmbsDict;
import com.au.sa.fssc.claim.base.config.dao.*;
import com.au.sa.fssc.claim.base.config.entities.*;
import com.au.sa.fssc.claim.base.config.repository.PmProjectRepository;
import com.au.sa.fssc.claim.base.config.repository.TCoItemLevel3Repository;
import com.au.sa.fssc.claim.base.config.repository.TCoItemlevel2Repository;
import com.au.sa.fssc.claim.base.constans.SystemConstants;
import com.au.sa.fssc.claim.base.sys.entities.TAppTokenValidity;
import com.au.sa.fssc.claim.base.sys.entities.TRmbsTaxTransferControl;
import com.au.sa.fssc.claim.base.sys.repository.TAppTokenValidityRepository;
import com.au.sa.fssc.claim.base.sys.repository.TRmbsTaxTransferControlRepository;
import com.au.sa.fssc.claim.base.sys.vo.MobileNextActivityVo;
import com.au.sa.fssc.claim.base.util.AppError;
import com.au.sa.fssc.claim.img.dao.impl.AppInvoiceDaoImpl;
import com.au.sa.fssc.claim.img.entities.OcrCallbackOcrinvoice;
import com.au.sa.fssc.claim.img.entities.TAppInvoiceFolder;
import com.au.sa.fssc.claim.img.entities.TClaimIinterlliConf;
import com.au.sa.fssc.claim.img.enums.AppInvoiceFolderEnum;
import com.au.sa.fssc.claim.img.param.*;
import com.au.sa.fssc.claim.img.repository.TClaimIinterlliConfRepository;
import com.au.sa.fssc.claim.img.service.IntellijFillClaimService;
import com.au.sa.fssc.claim.img.service.SceneFillClaimService;
import com.au.sa.fssc.common.sys.bo.UserObject;
import com.au.sa.fssc.common.sys.entities.SysGroup;
import com.au.sa.fssc.common.sys.entities.SysUser;
import com.au.sa.fssc.common.sys.repository.SysGroupRepository;
import com.au.sa.fssc.common.sys.repository.SysUserRepository;
import com.au.sa.fssc.common.util.bean.OrikaBeanUtil;
import com.au.sa.fssc.common.util.exception.BusinessException;
import com.au.sa.fssc.common.util.model.PageRequest;
import com.au.sa.fssc.common.util.model.Result;
import com.google.common.base.Joiner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.au.sa.fssc.claim.img.entities.TAppInvoiceFolder.*;


@Service
@Slf4j
public class SceneFillClaimServiceImpl  implements SceneFillClaimService {

    @Autowired
    TClaimIinterlliConfRepository claimIinterlliConfRepository;
    @Autowired
    AppInvoiceDaoImpl iAppInvoiceDao;

    @Resource
    SysGroupRepository sysGroupRepository;

    @Autowired
    ITRmbsOuTaxDao ouTaxDao;

    @Resource
    ITCoComsegcodeDao coComSegCodeDao;
    @Autowired
    IntellijFillClaimService intellijFillClaimService;
    @Resource
    TCoItemlevel2Repository level2Repository;
    @Resource
    TCoItemLevel3Repository level3Repository;

    @Resource
    TAppTokenValidityRepository appTokenValidityRepository;

    @Autowired
    private RmbsClaimRepository rmbsClaimRepository;

    @Autowired
    private RmbsClaimLineRepository claimLineRepository;

    @Resource(name = "baseClaimServiceImpl")
    IBaseClaimService baseClaimService;

    @Autowired
    IBaseClaimProcessService baseClaimProcessService;

    @Resource
    protected IBaseClaimDeleteService deleteClaimService;
    @Autowired
    ITBlacklistDao blacklistDao;

    @Autowired
    PmProjectRepository pmProjectRepository;

    @Autowired
    ITCoItem2DeptcostDao coItem2DeptcostDao;

    @Autowired
    ITRmbsClaimLockService claimLockService;
    @Resource
    protected IBaseClaimRelService claimRelService;

    @Autowired
    IRmbsDictDao rmbsDictDao;

    @Autowired
    BpmApiService bpmApiService;

    @Autowired
    TProcessWiRecordRepository tProcessWiRecordRepository;

    @Autowired
    TRmbsTaxTransferControlRepository rmbsTaxTransferControlRepository;

    @Autowired
    SysUserRepository sysUserRepository;



    @Override
    public List getClaimOptions(GetClaimOptionsParam param, UserObject user) {

        List<TClaimIinterlliConf> TClaimIinterlliConfList = claimIinterlliConfRepository.findByFromSystem(param.getFromSystem());
//                (List<TClaimIinterlliConf>) coComSegCodeDao.find("from TClaimIinterlliConf where ( fromSystem = 'APP_PC' or  fromSystem = ? ) order by caseNo  ",new Object[]{fromSystem});
        Map map = new HashMap();
        //1440 共享会计专用大类业务人员还可以勾选到 22030021020801900003 经办人：00198646 报账场景选择TR：银行手续费到票调整业务（共享专用）
        boolean hasMenu = userHasMenu(user, "100905");

        List<TClaimIinterlliConf> result = new ArrayList<>();

        if (TClaimIinterlliConfList != null && TClaimIinterlliConfList.size() > 0) {
            for (TClaimIinterlliConf tClaimIinterlliConf : TClaimIinterlliConfList) {
                //                JSONObject jsonObject = new JSONObject();
//                jsonObject.put("caseNo", tClaimIinterlliConf.getCaseNo());
//                jsonObject.put("caseName", tClaimIinterlliConf.getCaseName());
//                jsonObject.put("itemId", tClaimIinterlliConf.getItemId());
                //1440 共享会计专用大类业务人员还可以勾选到 22030021020801900003 经办人：00198646 报账场景选择TR：银行手续费到票调整业务（共享专用）
                if (tClaimIinterlliConf.getItem3Id().equals("019301001")) {
                    if (hasMenu) {
                        result.add(tClaimIinterlliConf);
                    }
                } else {
                    result.add(tClaimIinterlliConf);
                }
            }
        }


        return result;
    }

    public boolean userHasMenu(UserObject user ,String functionId){
        String userFunctiRoleSql = "  select distinct  sur.role_id from sys_rolefunc src , sys_user_role sur" +
                "         where src.functionid = :functionid " +
                "         and src.roleid  = sur.role_id" +
                "         and sur.user_id = :userid  " +
                "         AND sur.group_id = :groupId ";
        Map paramMap =new HashMap();
        paramMap.put("functionid",functionId);
        paramMap.put("userid",user.getUserId());
        paramMap.put("groupId",user.getGroupId());
        List userRoles = iAppInvoiceDao.findBySql(userFunctiRoleSql, paramMap);

        return !CollectionUtils.isEmpty(userRoles);
    }


    /**
     * 获取智能报账详情
     *
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> getClaimTemplate(GetClaimTemplateParam param, UserObject user) {

        long orgId = user.getOrgId();
        long compId = user.getCurCompId();
        SysGroup group = sysGroupRepository.findByGroupId(user.getCurGroupId()).get(0);

        String item3Id = "";
        String item3Name = "";

        Long caseNo = param.getCaseNo();//加密串
        String invoiceIds = StrUtil.emptyToDefault(param.getInvoiceIds(),"");//发票id
        List<Long> invoiceIdList = Arrays.stream(invoiceIds.split(",")).map(Long::valueOf).collect(Collectors.toList());
        Map map = new HashMap();

        //初始化税号信息
        initTaxNumber(orgId,map);

        String itemId = "";
        String item2Id = "";

        try {
            List<String> folderTreeList = iAppInvoiceDao.getFolderTreeList(invoiceIds, null);
            invoiceIds =  Joiner.on(",").join(folderTreeList.toArray());

            String[] ids = invoiceIds.split(",");
            List<TAppInvoiceFolder> ObjList = iAppInvoiceDao.getTAppInvoiceFolderGroupList(invoiceIds);
            if(ids.length!=ObjList.size()){
               throw new BusinessException("当前选择的发票中 含有一图多张但是未被选择的 请重新选择发票");
            }

            List<TClaimIinterlliConf> TClaimIinterlliConfList =  claimIinterlliConfRepository.findByCaseNo(caseNo);

            if (TClaimIinterlliConfList == null || TClaimIinterlliConfList.isEmpty()) {
                throw new Exception("配置表数据已删除");
            }
            TClaimIinterlliConf tClaimIinterlliConf = TClaimIinterlliConfList.get(0);

            itemId = tClaimIinterlliConf.getItemId();
            item2Id = tClaimIinterlliConf.getItem2Id();
            item3Id = tClaimIinterlliConf.getItem3Id();
            item3Name = tClaimIinterlliConf.getItem3Name();
            String costReduceYn = tClaimIinterlliConf.getCostReduceYn();
            String draftSaveYn = tClaimIinterlliConf.getDraftSaveYn();
            String feeDistributionYn = tClaimIinterlliConf.getFeeDistributionYn();
            String writeOffYn = tClaimIinterlliConf.getWriteOffYn();

            /*{"lineAmount" 明细金额 taxLineAmount" 税金-金额 taxAmount" 税金-税额 taxInvoiceType" 税金-发票类型 taxRate“ 税金-税率 totalAmountTaxNum" 报账金额 }*/
            List<TAppInvoiceFolder> appInvoiceList =iAppInvoiceDao.repository.findByIds(invoiceIdList);

            Set<String> imageIdSet = new HashSet<>();
            if (appInvoiceList != null && appInvoiceList.size() > 0) {
                for (TAppInvoiceFolder tAppInvoiceFolderOcrReq : appInvoiceList) {
                    imageIdSet.add(tAppInvoiceFolderOcrReq.getImageId().toString());
                }
            }

            String[] imageIdArr = new String[imageIdSet.size()];
            imageIdSet.toArray(imageIdArr);

            TRmbsClaim tempClaim =new TRmbsClaim();
            tempClaim.setItem2Id(item2Id);
            tempClaim.setItemId(itemId);
            tempClaim.setUnitName(StrUtil.emptyToDefault(map.get("unitName").toString(),""));

            String imageIds = Joiner.on(",").join(imageIdArr);
            List<Map> maps = intellijFillClaimService.ocrCallBackInvoiceToTaxLine(null, imageIds, itemId, item2Id, tempClaim);

            BigDecimal lineSumAmount = BigDecimal.ZERO;
            BigDecimal claimSumAmount = BigDecimal.ZERO;
            String t019Flag = "";
            for (Map map1 : maps) {
                OcrCallbackOcrinvoice ocrInvoice = (OcrCallbackOcrinvoice) map1.get("ocrInvoice");
                String totalAmountTaxNum = map1.get("totalAmountTaxNum").toString();
                String lineAmount = map1.get("lineAmount").toString();
                if (totalAmountTaxNum != null && !totalAmountTaxNum.equals("")) {
                    claimSumAmount = claimSumAmount.add(new BigDecimal(totalAmountTaxNum));
                }
                if (lineAmount != null && !lineAmount.equals("")) {
                    lineSumAmount = lineSumAmount.add(new BigDecimal(lineAmount));
                }
            }



            Map template = new HashMap();
            template.put("itemId", itemId);
            template.put("item2Id", item2Id);
            if(Arrays.asList(TAppInvoiceFolder.specialT019).contains(item2Id)){
                Map rsMap = intellijFillClaimService.calculateClaimAmountForT019(maps);
                t019Flag = rsMap.get("t019Flag").toString();
                BigDecimal subtractionSumAmount = (BigDecimal) rsMap.get("subtractionSumAmount");
                lineSumAmount = (BigDecimal) rsMap.get("lineSumAmount");
                claimSumAmount = (BigDecimal)rsMap.get("claimSumAmount");
                template.put("subtractionSumAmount",subtractionSumAmount.toString());
            }
            TCoComsegcode coCom = coComSegCodeDao.getTCoComsegcodeGroup_t001(compId, orgId, user.getCurCostCenterId(), user.getCurGroupId(), user.getCurCostCenterName());

            Assert.notNull( coCom, "费用承担部门为空!" );

//            JSONObject rsJsonObj = new JSONObject();
//            TRmbsClaim tRmbsClaim = new TRmbsClaim();
//            AppClaimInfoVo claimInfoVo = new AppClaimInfoVo();

            DecimalFormat df = new DecimalFormat("#,###.00");
            String claimSumAmountView = df.format(claimSumAmount);

            TCoItemlevel3 level3 = null;
            for (int i = 1; i <= 9; i++) {
                level3 = level3Repository.findByItemIdAndCompId(item2Id.trim() + "00" + i, user.getCurCompId()).get(0);
                if (level3 != null) {
                    break;
                }
            }
            //36349 IOS极简报账+智能报账选择的打的费，费用承担部门应该是带出来报销人默认的HR部门，只有项目期间手机费是带的“不分明细”（王富丽）00134419
            String costSegCode = "";
            String costSeg = "";
            TCoItemlevel3 tCoItemlevel3 = level3Repository.findByItemIdAndCompId(item3Id, 0L).get(0);
            Long isDefCostCoDepartment = 0L;
            if(tCoItemlevel3 !=null && tCoItemlevel3.getIsDefcostcoDepartment() != null && tCoItemlevel3.getIsDefcostcoDepartment().equals("1") ){
                isDefCostCoDepartment = tCoItemlevel3.getIsDefcostcoDepartment();
            }
            if(isDefCostCoDepartment == 1L){
                costSegCode = "0";
                costSeg = "不分明细";
            }else{
                costSegCode =group.getCostCenter();
                costSeg =group.getCostCenterDesc();
            }

            String claimLineDesc = "";
            if (itemId.equals("T008") || itemId.equals("T007") || itemId.equals("T009") || itemId.equals("T011")) {
                claimLineDesc = costSeg + user.getFullName() + "报销" + item3Name;
            }
            if (!itemId.equals("T008") && !itemId.equals("T007") && !itemId.equals("T009") && !itemId.equals("T011")) {
                claimLineDesc = costSeg + "--" + user.getFullName() + "报" + item3Name;
            }

            if (TAppInvoiceFolder.ITEM2_ID_007059.equals(item2Id)) {
                claimLineDesc = "不分明细" + user.getFullName() + "报销" + item3Name;
            }
            //设置默认成本中心

            template.put("orgId", coCom.getOrgId());
            template.put("coSegCode", coCom.getCoSegCode());
            template.put("orgName", coCom.getOrgName());
            template.put("buSegCode", coCom.getBuSegCode());
            template.put("buSegName", coCom.getBuSegName());
            template.put("userId", user.getUserId());
            template.put("fullname", user.getFullName());

            template.put("claimLineDesc", claimLineDesc);
            template.put("costSeg", costSeg);
            template.put("costSegCode", costSegCode);
            template.put("curUserGroupId", user.getCurGroupId());
            template.put("amount", claimSumAmount.toString());
            template.put("amountView", claimSumAmountView);
            template.put("lineSumAmount", lineSumAmount);
            template.put("itemId", itemId);
            template.put("item2Id", item2Id);
            template.put("item3Id", item3Id);
            template.put("item3Name", item3Name);
            template.put("feeDistributionYn", feeDistributionYn);//费用拆分
            template.put("costReduceYn", costReduceYn);//费用核减
            template.put("writeOffYn", writeOffYn);//借款核销
            template.put("draftYn", draftSaveYn);//借款核销
            template.put("groupAttributeId", coCom.getGroupAttributeId());

            template.put("t019Flag",t019Flag);

            JSONObject template1 = getToAppTemplate(item2Id, template);


            Map inMap = new HashMap();
            inMap.put("userId",user.getUserId());
            //35972 智能报账选择影像夹中 文件夹，可以将文件夹中全部发票赋值到报账单上，非发票挂接电子影像。（吕越洋）
            inMap.put("idIn", invoiceIds);
            inMap.put("folderType", AppInvoiceFolderEnum.foloderType.INVOICE.code);
            PageRequest<InvoiceFolderQueryParam> page = new PageRequest<>();
            page.setPageSize(1000);
            List<InvoiceFolderQueryParam> tAppInvoiceList = iAppInvoiceDao.queryList(inMap, page).getContent();
            template1.put("data", tAppInvoiceList);//--列表数据

            map.put("result", template1);

        } catch (Exception e) {
            //手工回滚异常
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("错误",e);
            throw new BusinessException("场景查询失败！");
        }
        return map;
    }

    /**
     * 初始化税号信息
     */
    public void initTaxNumber(Long orgId, Map initValueMap)  {

        TRmbsOuTax ouTax = ouTaxDao.getUnitName(orgId);
        if (ouTax != null) {
            if (ouTax.getUnitName() != null) {
                initValueMap.put("unitName", ouTax.getUnitName());
            } else {
                initValueMap.put("unitName", "");
            }
            if (ouTax.getTaxNumber() != null) {
                initValueMap.put("taxNumber", ouTax.getTaxNumber());
            } else {
                initValueMap.put("taxNumber", "");
            }
        } else {
            initValueMap.put("unitName", "");
            initValueMap.put("taxNumber", "");
        }
    }

    public static JSONObject getToAppTemplate(String item2Id, Map map) {

        JSONObject rsJsonObj = new JSONObject();
        String orgId = map.get("orgId") != null ? map.get("orgId").toString() : "";
        String orgName = map.get("orgName") != null ? map.get("orgName").toString() : "";
        String buSegCode = map.get("buSegCode") != null ? map.get("buSegCode").toString() : "";
        String buSegName = map.get("buSegName") != null ? map.get("buSegName").toString() : "";
        String userId = map.get("userId") != null ? map.get("userId").toString() : "";
        String fullname = map.get("fullname") != null ? map.get("fullname").toString() : "";
        String costSeg = map.get("costSeg") != null ? map.get("costSeg").toString() : "";
        String costSegCode = map.get("costSegCode") != null ? map.get("costSegCode").toString() : "";
        String curUserGroupId = map.get("curUserGroupId") != null ? map.get("curUserGroupId").toString() : "";
        String amount = map.get("amount") != null ? map.get("amount").toString() : "";
        String amountView = map.get("amountView") != null ? map.get("amountView").toString() : "";
        String lineSumAmount = map.get("lineSumAmount") != null ? map.get("lineSumAmount").toString() : "";
        String itemId = map.get("itemId") != null ? map.get("itemId").toString() : "";
        String item3Id = map.get("item3Id") != null ? map.get("item3Id").toString() : "";
        String item3Name = map.get("item3Name") != null ? map.get("item3Name").toString() : "";
        String feeDistributionYn = map.get("feeDistributionYn") != null ? map.get("feeDistributionYn").toString() : "";//费用拆分
        String costReduceYn = map.get("costReduceYn") != null ? map.get("costReduceYn").toString() : "";//费用核减
        String writeOffYn = map.get("writeOffYn") != null ? map.get("writeOffYn").toString() : "";//借款核销
        String draftYn = map.get("draftYn") != null ? map.get("draftYn").toString() : "";//借款核销
        String groupAttributeId = map.get("groupAttributeId") != null ? map.get("groupAttributeId").toString() : "";
        String coSegCode = map.get("coSegCode") != null ? map.get("coSegCode").toString() : "";
        String claimLineDesc = map.get("claimLineDesc") != null ? map.get("claimLineDesc").toString() : "";
        String t019Flag = map.get("t019Flag") != null ? map.get("t019Flag").toString() : "";
        String costReduceDisabled = "N";
        if (!t019Flag.equals("")) {
            costReduceDisabled = "Y";
        }
        String subtractionSumAmount = map.get("subtractionSumAmount") != null ? map.get("subtractionSumAmount").toString() : "";
        //手机费
        switch (item2Id) {
            case ITEM2_ID_007049: {

                StringBuilder stringBuffer = new StringBuilder();
                stringBuffer.append("[");
                stringBuffer.append("{'index': '1','key': '公司OU',value:'" + orgId + "',text:'" + orgName + "',valueKey:'orgId','required': true,'editable': true,'inputType':'chosen'}  ");
                stringBuffer.append("{'index': '2','key': '所属事业部',value:'" + buSegCode + "',text:'" + buSegName + "',valueKey:'buSegCode','required': true,'editable': true,'inputType':'chosen'}  ");
                stringBuffer.append("{'index': '3','key': '报账金额合计',value:'" + amount + "',text:'" + amountView + "',valueKey:'payAmount','required': true,'editable': false,'inputType':'none'}  ");
                stringBuffer.append("{'index': '4','key': '报销人',value:'" + userId + "',text:'" + fullname + "',valueKey:'applyUserName','required': true,'editable': true,'inputType':'chosen'}  ");
                stringBuffer.append("]");
                rsJsonObj.put("baseData", JSONObject.parseArray(stringBuffer.toString()));

                stringBuffer = new StringBuilder();
                stringBuffer.append("{");
                stringBuffer.append("   ,feeDistributionYn:'" + feeDistributionYn + "'");//费用拆分

                stringBuffer.append("   ,costReduceDisabled:'" + costReduceDisabled + "'");//费用核减是否不可用

                stringBuffer.append("   ,costReduceYn:'" + costReduceYn + "'");//费用核减

                stringBuffer.append("   ,writeOffYn:'" + writeOffYn + "'");//借款核销

                stringBuffer.append("   ,draftYn:'" + draftYn + "'");//借款核销

                stringBuffer.append("   ,itemId:'" + itemId + "'");//借款核销

                stringBuffer.append("   ,item2Id:'" + item2Id + "'");//借款核销

                stringBuffer.append("   ,coSegCode:'" + coSegCode + "'");//借款核销

                stringBuffer.append("	}");
                rsJsonObj.put("claimInfoVo", JSONObject.parseObject(stringBuffer.toString()));

                stringBuffer = new StringBuilder();
                stringBuffer.append("[{");
                stringBuffer.append("	apProjectSeg:'不分明细' ");
                stringBuffer.append("	,apProjectSegCode:'0' ");
                stringBuffer.append("	,buSeg:'" + buSegName + "' ");
                stringBuffer.append("   ,buSegCode:'" + buSegCode + "'  ");
                stringBuffer.append("	,claimId:'0L' ");
                stringBuffer.append("	,claimLineDesc:'" + claimLineDesc + "' ");
                stringBuffer.append("	,claimLineId:'0L' ");
                stringBuffer.append("	,costAssumeDepartment:'" + curUserGroupId + "' ");
                stringBuffer.append("	,costSeg:'" + costSeg + "' ");
                stringBuffer.append("	,costSegCode:'" + costSegCode + "' ");
                stringBuffer.append("	,foreignApplyAmount:'" + lineSumAmount + "' ");
                stringBuffer.append("	,groupAttributeId:'" + groupAttributeId + "' ");
                stringBuffer.append("	,item3Id:'" + item3Id + "' ");
                stringBuffer.append("	,item3Name:'" + item3Name + "' ");
                stringBuffer.append("	,vendorSiteId:null ");
                stringBuffer.append("	}]");
                rsJsonObj.put("lineList", JSONObject.parseArray(stringBuffer.toString()));

                return rsJsonObj;

            }
            case ITEM2_ID_007059: {

                StringBuilder stringBuffer = new StringBuilder();
                stringBuffer.append("[");
                stringBuffer.append("{'index': '1','key': '公司OU',value:'" + orgId + "',text:'" + orgName + "',valueKey:'orgId','required': true,'editable': true,'inputType':'chosen'}  ");
                stringBuffer.append("{'index': '2','key': '所属事业部',value:'" + buSegCode + "',text:'" + buSegName + "',valueKey:'buSegCode','required': true,'editable': true,'inputType':'chosen'}  ");
                stringBuffer.append("{'index': '3','key': '报账金额合计',value:'" + amount + "',text:'" + amountView + "',valueKey:'payAmount','required': true,'editable': false,'inputType':'none'}  ");
                stringBuffer.append("{'index': '4','key': '报销人',value:'" + userId + "',text:'" + fullname + "',valueKey:'applyUserName','required': true,'editable': true,'inputType':'chosen'}  ");

                //-- 35722 pc 影像夹 智能报账时 保存草稿时 提示信息不正确 项目名称 编号  不能是 不分明细
                stringBuffer.append("{'index': '5','key': '项目名称',value:'',text:'',valueKey:'projectName','required': true,'editable': true,'inputType':'chosen'}  ");
                stringBuffer.append("{'index': '6','key': '项目编号',value:'',text:'',valueKey:'projectNum','required': true,'editable': false,'inputType':'none'}  ");
                stringBuffer.append("]");
                rsJsonObj.put("baseData", JSONObject.parseArray(stringBuffer.toString()));

                stringBuffer = new StringBuilder();
                stringBuffer.append("{");
                stringBuffer.append("   ,feeDistributionYn:'" + feeDistributionYn + "'");//费用拆分

                stringBuffer.append("   ,costReduceDisabled:'" + costReduceDisabled + "'");//费用核减是否不可用

                stringBuffer.append("   ,costReduceYn:'" + costReduceYn + "'");//费用核减

                stringBuffer.append("   ,writeOffYn:'" + writeOffYn + "'");//借款核销

                stringBuffer.append("   ,draftYn:'" + draftYn + "'");//借款核销

                stringBuffer.append("   ,itemId:'" + itemId + "'");//借款核销

                stringBuffer.append("   ,item2Id:'" + item2Id + "'");//借款核销

                stringBuffer.append("   ,coSegCode:'" + coSegCode + "'");//借款核销

                stringBuffer.append("   ,projectId:''");//借款核销

                stringBuffer.append("	}");
                rsJsonObj.put("claimInfoVo", JSONObject.parseObject(stringBuffer.toString()));

                stringBuffer = new StringBuilder();
                stringBuffer.append("[{");
                stringBuffer.append("	apProjectSeg:'不分明细' ");
                stringBuffer.append("	,apProjectSegCode:'0' ");
                stringBuffer.append("	,buSeg:'" + buSegName + "' ");
                stringBuffer.append("   ,buSegCode:'" + buSegCode + "'  ");
                stringBuffer.append("	,claimId:'0L' ");
                stringBuffer.append("	,claimLineDesc:'" + claimLineDesc + "' ");
                stringBuffer.append("	,claimLineId:'0L' ");
                stringBuffer.append("	,costAssumeDepartment:'" + curUserGroupId + "' ");
                stringBuffer.append("	,costSeg:'不分明细' ");
                stringBuffer.append("	,costSegCode:'0' ");
                stringBuffer.append("	,foreignApplyAmount:'" + lineSumAmount + "' ");
                stringBuffer.append("	,groupAttributeId:'" + groupAttributeId + "' ");
                stringBuffer.append("	,item3Id:'" + item3Id + "' ");
                stringBuffer.append("	,item3Name:'" + item3Name + "' ");
                stringBuffer.append("	,vendorSiteId:null ");
                stringBuffer.append("	}]");

                rsJsonObj.put("lineList", JSONObject.parseArray(stringBuffer.toString()));
                return rsJsonObj;

            }
            case ITEM2_ID_007050:
            case ITEM2_ID_007013: {

                StringBuilder stringBuffer = new StringBuilder();
                stringBuffer.append("[");
                stringBuffer.append("{'index': '1','key': '公司OU',value:'" + orgId + "',text:'" + orgName + "',valueKey:'orgId','required': true,'editable': true,'inputType':'chosen'}  ");
                stringBuffer.append("{'index': '2','key': '所属事业部',value:'" + buSegCode + "',text:'" + buSegName + "',valueKey:'buSegCode','required': true,'editable': true,'inputType':'chosen'}  ");
                stringBuffer.append("{'index': '3','key': '报账金额合计',value:'" + amount + "',text:'" + amountView + "',valueKey:'payAmount','required': true,'editable': false,'inputType':'none'}  ");
                stringBuffer.append("{'index': '4','key': '报销人',value:'" + userId + "',text:'" + fullname + "',valueKey:'applyUserName','required': true,'editable': true,'inputType':'chosen'}  ");
//			stringBuffer.append("{'index': '5','key': '项目名称',value:'不分明细',text:'不分明细',valueKey:'projectName','required': true,'editable': true,'inputType':'chosen'}  ");
//			stringBuffer.append("{'index': '6','key': '项目编号',value:'0',text:'0',valueKey:'projectNum','required': true,'editable': false,'inputType':'none'}  ");
                stringBuffer.append("]");
                rsJsonObj.put("baseData", JSONObject.parseArray(stringBuffer.toString()));

                stringBuffer = new StringBuilder();
                stringBuffer.append("{");
                stringBuffer.append("   ,feeDistributionYn:'" + feeDistributionYn + "'");//费用拆分

                stringBuffer.append("   ,costReduceDisabled:'" + costReduceDisabled + "'");//费用核减是否不可用

                stringBuffer.append("   ,costReduceYn:'" + costReduceYn + "'");//费用核减

                stringBuffer.append("   ,writeOffYn:'" + writeOffYn + "'");//借款核销

                stringBuffer.append("   ,draftYn:'" + draftYn + "'");//借款核销

                stringBuffer.append("   ,itemId:'" + itemId + "'");//借款核销

                stringBuffer.append("   ,item2Id:'" + item2Id + "'");//借款核销

                stringBuffer.append("   ,coSegCode:'" + coSegCode + "'");//借款核销

                stringBuffer.append("	}");
                rsJsonObj.put("claimInfoVo", JSONObject.parseObject(stringBuffer.toString()));

                stringBuffer = new StringBuilder();
                stringBuffer.append("[{");
                stringBuffer.append("	apProjectSeg:'不分明细' ");
                stringBuffer.append("	,apProjectSegCode:'0' ");
                stringBuffer.append("	,buSeg:'" + buSegName + "' ");
                stringBuffer.append("   ,buSegCode:'" + buSegCode + "'  ");
                stringBuffer.append("	,claimId:'0L' ");
                stringBuffer.append("	,claimLineDesc:'" + claimLineDesc + "' ");
                stringBuffer.append("	,claimLineId:'0L' ");
                stringBuffer.append("	,costAssumeDepartment:'" + curUserGroupId + "' ");
                stringBuffer.append("	,costSeg:'" + costSeg + "' ");
                stringBuffer.append("	,costSegCode:'" + costSegCode + "' ");
                stringBuffer.append("	,foreignApplyAmount:'" + lineSumAmount + "' ");
                stringBuffer.append("	,groupAttributeId:'" + groupAttributeId + "' ");
                stringBuffer.append("	,item3Id:'" + item3Id + "' ");
                stringBuffer.append("	,item3Name:'" + item3Name + "' ");
                stringBuffer.append("	,vendorSiteId:null ");
                stringBuffer.append("	}]");

                rsJsonObj.put("lineList", JSONObject.parseArray(stringBuffer.toString()));
                return rsJsonObj;
            }
            case ITEM2_ID_019302:
            case ITEM2_ID_019301: {

                StringBuilder stringBuffer = new StringBuilder();
                stringBuffer.append("[");
                stringBuffer.append("{'index': '1','key': '公司OU',value:'" + orgId + "',text:'" + orgName + "',valueKey:'orgId','required': true,'editable': true,'inputType':'chosen'}  ");
                stringBuffer.append("{'index': '2','key': '所属事业部',value:'" + buSegCode + "',text:'" + buSegName + "',valueKey:'buSegCode','required': true,'editable': true,'inputType':'chosen'}  ");
                stringBuffer.append("{'index': '3','key': '报账金额合计',value:'" + amount + "',text:'" + amountView + "',valueKey:'payAmount','required': true,'editable': false,'inputType':'none'}  ");
                //stringBuffer.append("{'index': '4','key': '报销人',value:'"+userId+"',text:'"+fullname+"',valueKey:'applyUserName','required': true,'editable': true,'inputType':'chosen'}  ");
                stringBuffer.append("]");
                rsJsonObj.put("baseData", JSONObject.parseArray(stringBuffer.toString()));

                stringBuffer = new StringBuilder();
                stringBuffer.append("{");
                stringBuffer.append("   ,feeDistributionYn:'" + feeDistributionYn + "'");//费用拆分

                stringBuffer.append("   ,costReduceDisabled:'" + costReduceDisabled + "'");//费用核减是否不可用

                stringBuffer.append("   ,costReduceYn:'" + costReduceYn + "'");//费用核减

                stringBuffer.append("   ,writeOffYn:'" + writeOffYn + "'");//借款核销

                stringBuffer.append("   ,draftYn:'" + draftYn + "'");//借款核销

                stringBuffer.append("   ,itemId:'" + itemId + "'");//借款核销

                stringBuffer.append("   ,item2Id:'" + item2Id + "'");//借款核销

                stringBuffer.append("   ,coSegCode:'" + coSegCode + "'");//借款核销

                stringBuffer.append("   ,subtractionSumAmount:'" + subtractionSumAmount + "'");//借款核销

                stringBuffer.append("	}");
                rsJsonObj.put("claimInfoVo", JSONObject.parseObject(stringBuffer.toString()));

                if (t019Flag.equals("")) {
                    stringBuffer = new StringBuilder();
                    stringBuffer.append("[{");
                    stringBuffer.append("	apProjectSeg:'不分明细' ");
                    stringBuffer.append("	,apProjectSegCode:'0' ");
                    stringBuffer.append("	,buSeg:'" + buSegName + "' ");
                    stringBuffer.append("   ,buSegCode:'" + buSegCode + "'  ");
                    stringBuffer.append("	,claimId:'0L' ");
                    stringBuffer.append("	,claimLineDesc:'" + claimLineDesc + "' ");
                    stringBuffer.append("	,claimLineId:'0L' ");
                    stringBuffer.append("	,costAssumeDepartment:'" + curUserGroupId + "' ");
                    stringBuffer.append("	,costSeg:'" + costSeg + "' ");
                    stringBuffer.append("	,costSegCode:'" + costSegCode + "' ");
                    stringBuffer.append("	,foreignApplyAmount:'" + lineSumAmount + "' ");
                    stringBuffer.append("	,groupAttributeId:'" + groupAttributeId + "' ");
                    stringBuffer.append("	,item3Id:'" + item3Id + "' ");
                    stringBuffer.append("	,item3Name:'" + item3Name + "' ");
                    stringBuffer.append("	,vendorSiteId:null ");
                    stringBuffer.append("	}]");
                } else if (t019Flag.equals("specialOnly")) {
                    stringBuffer = new StringBuilder();
                    stringBuffer.append("[{");
                    stringBuffer.append("	apProjectSeg:'不分明细' ");
                    stringBuffer.append("	,apProjectSegCode:'0' ");
                    stringBuffer.append("	,buSeg:'" + buSegName + "' ");
                    stringBuffer.append("   ,buSegCode:'" + buSegCode + "'  ");
                    stringBuffer.append("	,claimId:'0L' ");
                    stringBuffer.append("	,claimLineDesc:'" + claimLineDesc + "' ");
                    stringBuffer.append("	,claimLineId:'0L' ");
                    stringBuffer.append("	,costAssumeDepartment:'" + curUserGroupId + "' ");
                    stringBuffer.append("	,costSeg:'" + costSeg + "' ");
                    stringBuffer.append("	,costSegCode:'" + costSegCode + "' ");
                    stringBuffer.append("	,foreignApplyAmount:'" + lineSumAmount + "' ");
                    stringBuffer.append("	,groupAttributeId:'" + groupAttributeId + "' ");
                    stringBuffer.append("	,item3Id:'" + item3Id + "' ");
                    stringBuffer.append("	,item3Name:'" + item3Name + "' ");
                    stringBuffer.append("	,vendorSiteId:null ");
                    stringBuffer.append("	}]");
                } else if (t019Flag.equals("otherOnly")) {
                    stringBuffer = new StringBuilder();
                    stringBuffer.append("[{");
                    stringBuffer.append("	apProjectSeg:'不分明细' ");
                    stringBuffer.append("	,apProjectSegCode:'0' ");
                    stringBuffer.append("	,buSeg:'" + buSegName + "' ");
                    stringBuffer.append("   ,buSegCode:'" + buSegCode + "'  ");
                    stringBuffer.append("	,claimId:'0L' ");
                    stringBuffer.append("	,claimLineDesc:'" + claimLineDesc + "' ");
                    stringBuffer.append("	,claimLineId:'0L' ");
                    stringBuffer.append("	,costAssumeDepartment:'" + curUserGroupId + "' ");
                    stringBuffer.append("	,costSeg:'" + costSeg + "' ");
                    stringBuffer.append("	,costSegCode:'" + costSegCode + "' ");
                    stringBuffer.append("	,foreignApplyAmount:'" + lineSumAmount + "' ");
                    stringBuffer.append("	,groupAttributeId:'" + groupAttributeId + "' ");
                    stringBuffer.append("	,item3Id:'" + item3Id + "' ");
                    stringBuffer.append("	,item3Name:'" + item3Name + "' ");
                    stringBuffer.append("	,vendorSiteId:null ");
                    stringBuffer.append("	},");
                    stringBuffer.append(" {");
                    stringBuffer.append("	apProjectSeg:'不分明细' ");
                    stringBuffer.append("	,apProjectSegCode:'0' ");
                    stringBuffer.append("	,buSeg:'" + buSegName + "' ");
                    stringBuffer.append("   ,buSegCode:'" + buSegCode + "'  ");
                    stringBuffer.append("	,claimId:'0L' ");
                    stringBuffer.append("	,claimLineDesc:'" + claimLineDesc + "' ");
                    stringBuffer.append("	,claimLineId:'0L' ");
                    stringBuffer.append("	,costAssumeDepartment:'" + curUserGroupId + "' ");
                    stringBuffer.append("	,costSeg:'" + costSeg + "' ");
                    stringBuffer.append("	,costSegCode:'" + costSegCode + "' ");
                    stringBuffer.append("	,foreignApplyAmount:'" + "-" + lineSumAmount + "' ");
                    stringBuffer.append("	,groupAttributeId:'" + groupAttributeId + "' ");
                    stringBuffer.append("	,item3Id:'" + item3Id + "' ");
                    stringBuffer.append("	,item3Name:'" + item3Name + "' ");
                    stringBuffer.append("	,vendorSiteId:null ");
                    stringBuffer.append("	}]");

                }
                rsJsonObj.put("lineList", JSONObject.parseArray(stringBuffer.toString()));
                return rsJsonObj;
            }
        }
        return null;
    }

    /**
     * 发送
     *
     */
    @Override
    public Result<Map> appSendClaim(AppSendClaimParam param, UserObject user) {

        String userId=user.getUserId().toString();
        String mobileId = param.getMobileId();//手机ID
        String ciphertext = param.getCiphertext();//加密串

        //判断是否有效

//        try{
            TAppTokenValidity tAppTokenValidity = new TAppTokenValidity();
            tAppTokenValidity.setToken(ciphertext);
            tAppTokenValidity.setUserId(userId);
            appTokenValidityRepository.save(tAppTokenValidity);
//        } catch (Exception e){
//            e.printStackTrace();
//            Map<String,Object> map = new HashMap<String,Object>();
//            map.put("code", 1);//1：失败
//            map.put("result", null);//查询结果
//            map.put("message", "加密验证失败");//错误信息
//            map.put("success", false);//成功标志
//            String str = JSONObject.toJSONString(map, SerializerFeature.WriteMapNullValue);
//            out.print(URLEncoder.encode(str, "UTF-8"));
//            return;
//        }

        Map<String,Object> resultMap= new HashMap<>();
        String str=null;
        String pendingId=null;
        String nextActivityInstID=null;
        boolean success=true;
        String code="0";//0:正常，106：预算校验提示，1：错误,107:报账单校验提示
        String massage=null;
        Long claimId=param.getClaimId();
        //预算校验提示确认
        String budgetConfirm=StrUtil.emptyToDefault(param.getBudgetConfirm(),"");
        //报账单校验提示确认
        String claimConfirm=StrUtil.emptyToDefault(param.getClaimConfirm(),"");

        TRmbsClaim claim=rmbsClaimRepository.findByClaimId(claimId);

        //-- 智能报账验证 黑名单
        if ( claim.getImageMountMode()!=null&& !"PS-1".equals(claim.getImageMountMode()) ) {


//            ITAppInvoiceFolderBlacklistDao blacklistDao = (ITAppInvoiceFolderBlacklistDao) AppBeanFactory.getBean("appInvoiceFolderBlacklistDao");
//            IAppInvoiceDao iAppInvoiceDao = (IAppInvoiceDao) AppBeanFactory.getBean("iAppInvoiceDao");

            //-- 黑名单逻辑 : 当前人如果被设置成了黑名单 则不运行智能报账
            String reasonForDisabling ;

            //-- 查询 是否补单
            if (iAppInvoiceDao.isSupplementClaim(claimId)) {
                reasonForDisabling = blacklistDao.isBlack( Long.valueOf( userId ), claimId, false);
            }else{
                reasonForDisabling = blacklistDao.isBlack( Long.valueOf( userId ), null, false);
            }

            if( null != reasonForDisabling ){
                resultMap.put("code", "109");
                resultMap.put("pendingId", pendingId);
                resultMap.put("nextActivityInstID", nextActivityInstID);
                resultMap.put("reasonForDisabling", reasonForDisabling);

                return Result.failure(resultMap);
            }

            if(claim.getImageMountMode() != null && "PD-1".equals(claim.getImageMountMode())){
                List<TRmbsClaimLine> lineList = claimLineRepository.findByClaimId(claimId);
                if (lineList!=null && lineList.size()>0) {
                    for (TRmbsClaimLine tRmbsClaimLine : lineList) {
                        List<TCoItemlevel3> level3 = level3Repository.findByItemIdAndDeptCostTypeCode(tRmbsClaimLine.getItem3Id(), tRmbsClaimLine.getGroupAttributeId());
//                                iAppInvoiceDao.find("from TCoItemlevel3 where itemId = '" + tRmbsClaimLine.getItem3Id() + "' and deptCostTypeCode='" + tRmbsClaimLine.getGroupAttributeId() + "'");
                        if (level3 == null) {
                            resultMap.put("code", "109");
                            resultMap.put("pendingId", pendingId);
                            resultMap.put("nextActivityInstID", nextActivityInstID);
                            resultMap.put("reasonForDisabling", "该报账场景的费用承担部门和业务类型不匹配，请检查!");

                            return Result.failure(resultMap);
                        }
                    }
                }

            }

        }

//       SubmitClaimService service =(SubmitClaimService)AppBeanFactory.getBean("submit"+claim.getItemId()+"ClaimService-gbs");
//        ProcessCreateFacadeService processCreateFacadeService= (ProcessCreateFacadeService) AppBeanFactory.getBean("processCreateFacadeService");
// TODO 一期不走预算校验      if(!budgetConfirm.equals("Y")){//如果确认提示则不再走此校验
//
//            mo.setInput("claimId", claimId);
//            mo.setInput("itemId", claim.getItemId());
//            mo.setInput("appSend", "Y");
//            //43041 着急，报账单22060021090604800009原来由APP端上传影像，现在补单环节，无法补传影像
//            String imageMountMode = claim.getImageMountMode();
//            mo.setInput("imageMountMode",imageMountMode);
//            str=service.valiBudget(mo);//flag,S:建议,C:强控,U不控制
//            try {
//                Document doc = DocumentHelper.parseText(str);
//                String codeStr=doc.getRootElement().elementText("rsflag");
//                massage=doc.getRootElement().elementText("rsMessage");
//
//                if(codeStr.equals("U")){
//                    code="0";
//                }else if(codeStr.equals("S")){
//                    code="106";
//                    success=false;
//                }else if(codeStr.equals("C")){
//                    code="1";
//                    success=false;
//                }
//            } catch (DocumentException e) {
//                code="1";
//                massage="预算校验出错！";
//                success=false;
//                e.printStackTrace();
//            }
//        }

        //报账单校验
        if(!claimConfirm.equals("Y")){
            if(code.equals("0")){
               Map validateMap = new HashMap();
                try {

                   validateMap.put("claimId", claimId);
                   validateMap.put("itemId", claim.getItemId());
                   validateMap.put("item2Id", claim.getItem2Id());
                   validateMap.put("claimNo", claim.getClaimNo());
                   validateMap.put("processStateEng", claim.getProcessStateEng());
                   validateMap.put("payAmount", String.valueOf(claim.getPayAmount()));
                   validateMap.put("otherInvoiceAmount",String.valueOf(claim.getOtherInvoiceAmount()));
                   validateMap.put("compId", String.valueOf(claim.getCompId()));
                   validateMap.put("orgId", String.valueOf(claim.getOrgId()));
                   validateMap.put("appSend", "Y");
                   validateMap.put("applyAmount", String.valueOf(claim.getApplyAmount()));
                   validateMap.put("adjustApplyAmount", String.valueOf(claim.getAdjustApplyAmount()));
                   validateMap.put("verifInstructions", claim.getVerifInstructions());
                   validateMap.put("vendorNo",claim.getVendorNo());
                   validateMap.put("contractNo", claim.getContractNo());
                   validateMap.put("buSegCode", claim.getBuSegCode());
                   validateMap.put("vendorSiteId", Optional.ofNullable(claim.getVendorSiteId()).orElse(0));
                    //43041 着急，报账单22060021090604800009原来由APP端上传影像，现在补单环节，无法补传影像
                    validateMap.put("imageMountMode", StrUtil.trimToEmpty(claim.getImageMountMode()));
                    if(claim.getExpenseIssuerId()!=null&&!claim.getExpenseIssuerId().equals("")){
                        validateMap.put("expenseIssuerId", String.valueOf(claim.getExpenseIssuerId()));
                        validateMap.put("expenseIssuerName", claim.getExpenseIssuerName());
                    }


                    Collection<AppError>  errList =  baseClaimProcessService.validate(OrikaBeanUtil.map(validateMap,ClaimPageParams.class),user);
                    if(errList!=null&&errList.size()>0){
                        for (AppError err : errList) {
                            if(err.getInfoGrade().name().equals("ERROR")){
                                code="109";
                                success=false;
                            }


                            //是极简报账时，才会进行校验
                            if ("PD-1".equals(claim.getImageMountMode())) {
                                if (SystemConstants.WRITED_ERROR_MESSAGE_1.equals(err.getCode())
                                        || SystemConstants.WRITED_ERROR_MESSAGE_2.equals(err.getCode())
                                        || SystemConstants.WRITED_ERROR_MESSAGE_3.equals(err.getCode())
                                        || SystemConstants.WRITED_ERROR_MESSAGE_4.equals(err.getCode())) {

                                    if (StrUtil.isBlank(claim.getVerifInstructions())) {//第一次发送
                                        return handleCode110Data( err);
                                    }else{//保存信息后发送
                                        if (!SystemConstants.WRITED_ERROR_MESSAGE_4.equals(err.getCode())) {
                                            return handleCode110Data( err);
                                        }
                                    }

                                }

                            }

                            if(massage==null || "".equals(massage)){
                                massage=err.getCode();
                            }else{
                                massage=massage+"\n\n"+err.getCode();
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("校验错误",e);
//                    code="107";
//                    success=false;

//                  else{
                        code="109";
                        success=false;
                        if(massage==null || "".equals(massage)){
                            massage=e.getLocalizedMessage();
                        }else{
                            massage=massage+"\n\n"+e.getLocalizedMessage();
                        }
//                    }
//                    e.printStackTrace();
                }
            }
        }


// TODO 规则引擎       String ruleEngineFlg = request.getParameter("ruleEngineFlg");
//        if ( "0".equals(code) &&  StringUtils.isBlank(ruleEngineFlg)){
//
//            //-- 规则引擎 相关
//            //-- 是否校验规则引擎 : 规则引擎弱控 点继续
//            //-------------------------------流程引擎获取发票规则校验结果----------------------------
//            EngineManagerService engineManagerService = (EngineManagerService) AppBeanFactory.getBean("engineManagerService");
//
//            Map engineMoMap = new HashMap();
//            if(claim != null){
//                engineMoMap.put("claimId", nullToBlank(claim.getClaimId()));
//                engineMoMap.put("claimNo", claim.getClaimNo());
//                engineMoMap.put("itemId", claim.getItemId());
//                engineMoMap.put("item2Id", claim.getItem2Id());
//                engineMoMap.put("claimNo", claim.getClaimNo());
//            }
//
//
//            IMessageObject engineMo = new MessageObject(engineMoMap);
//            try {
//
//                UserObject user = null;
//                try {
//                    user = AppUtil.getUserInfoByUserId(claim.getApplyUserId());
//                } catch (DBException e1) {
//                    e1.printStackTrace();
//                }
//                Locale locale = new Locale("zh_CN");
//
//                engineMo.setUser(user);
//                engineMo.setLocale(locale);
//
//                engineManagerService.engineRuleResultView(engineMo);
//                if(engineMo.getInputMap().containsKey("tEngineExeLog")){
//                    TEngineExeLog tEngineExeLog = (TEngineExeLog) engineMo.getInput("tEngineExeLog");
//                    engineManagerService.saveEngineRuleLog(tEngineExeLog);
//                }
//            } catch (BusinessException e1) {
//                e1.printStackTrace();
//            } catch (InvokeException e1) {
//                e1.printStackTrace();
//            }
//
//
//            Map<String, EnineRuleNoteVo> validateSummaryMap = (Map<String, EnineRuleNoteVo>) engineMo.getOutput();
//
//            boolean submitFlag = false;
//            String tmpValidateStatus = OcrValidationConfig.NOTE_OK;
//            for(EnineRuleNoteVo tmpEnineRuleNoteVo : validateSummaryMap.values()){
//                if(tmpEnineRuleNoteVo.isSubmitBlockFlag()){
//                    submitFlag = true;
//                }
//
//                //设置级别
//                if(StringUtils.isBlank(tmpValidateStatus)){
//                    tmpValidateStatus = tmpEnineRuleNoteVo.getValidateStatus();
//                }else if(OcrValidationConfig.NOTE_ERROR.equals(tmpEnineRuleNoteVo.getValidateStatus())){
//                    tmpValidateStatus = tmpEnineRuleNoteVo.getValidateStatus();
//                }else if(OcrValidationConfig.NOTE_WARNING.equals(tmpEnineRuleNoteVo.getValidateStatus())
//                        && tmpValidateStatus.equals(OcrValidationConfig.NOTE_OK)){
//                    tmpValidateStatus = tmpEnineRuleNoteVo.getValidateStatus();
//                }
//            }
//
//            if ( !OcrValidationConfig.NOTE_OK.equals(tmpValidateStatus) ) {
//
//                //-- 规则引擎验证不通过 或者 警告
//                Map<String, Object> result = new HashMap<String, Object>();
//                //-- 错误代码 : error 不通过 ,  warning 警告
//                if(OcrValidationConfig.NOTE_WARNING.equals(tmpValidateStatus)){
//                    result.put("errorCode", "WARNING");
//                }else if(OcrValidationConfig.NOTE_ERROR.equals(tmpValidateStatus)){
//                    result.put("errorCode", "ERROR");
//                }
//
//                result.put("ruleEngine", validateSummaryMap.values()); //-- 规则引擎列表
//
//
//                map.put("code", "111");
//                map.put("message", "");
//                map.put("result", result);
//                map.put("success", false);
//                String claimStr = JSONObject.toJSONString(map, SerializerFeature.WriteMapNullValue);
//                out.print(URLEncoder.encode(claimStr, "UTF-8"));
//                return;
//            }
//
//        }



        if(claim.getProcessInstanceId()==null || "rootDrafterActivity".equals(claim.getProcessStateEng())){
            if(code.equals("0")){
//                IMessageObject mo2=new MessageObject();
//                SysUserGroupDaoImpl sysUserGroupDao=(SysUserGroupDaoImpl) AppBeanFactory.getBean("sysUserGroupDao");
//                SysUser user=sysUserGroupDao.get(SysUser.class, claim.getApplyUserId());
//                SysGroup group=sysUserGroupDao.get(SysGroup.class, claim.getApplyDeptId());
//                SysUserGroup userGroup=(SysUserGroup) sysUserGroupDao.isExistUserIdAndGroupId(user.getUserid(), group.getGroupid()).get(0);
//                UserObject userObject=new UserObject();
//                userObject.setUserid(user.getUserid());
//                userObject.setUsername(user.getUsername());
//                userObject.setFullname(user.getFullname());
//                userObject.setCurGroupId(String.valueOf(claim.getApplyDeptId()));
//                userObject.setCurGroupName(claim.getApplyDeptName());
//                userObject.setGroupPath(group.getGroupPath());
//                userObject.setCurCompId(String.valueOf(userGroup.getCompid()));
//                userObject.setCurCompName(userGroup.getCompName());
//                userObject.setCurSecondaryCompId(String.valueOf(claim.getApplyComId()));
//                userObject.setCurSecondaryCompName(claim.getApplyComName());
//                userObject.setCurSecondDeptId(String.valueOf(claim.getApplySecondDeptId()));
//                userObject.setCurSecondDeptName(claim.getApplySecondDeptName());
//
//                mo2.setInput("claimId", claimId);
//                mo2.setInput("itemId", claim.getItemId());
//                mo2.setInput("appSend", "Y");
//                mo2.setInput("item2Id", claim.getItem2Id());
//                mo2.setUser(userObject);
//                String serverAddress = ConfigProperties.getConfig("CUSFS_SERVER_ADDRESS");
//                mo2.setServerAddress(serverAddress);

                try {
                    CallProcessRequestDto requestDto = new CallProcessRequestDto();
                    CallProcessVariables processVariables = new CallProcessVariables();
                    processVariables.setClaimID(claimId.toString());
                    processVariables.setItemID(claim.getItemId());
                    processVariables.setItem2ID( claim.getItem2Id());
                    processVariables.setCurUserID(userId);
                    processVariables.setCurUserName(user.getUserName());
                    processVariables.setBizState(String.valueOf(TProcessWiparticipantRecord.BIZSTATE_ROOT));
                    processVariables.setOrgId(claim.getOrgId().toString());



//                    requestDto.setFormData();
                    requestDto.setTitle(claim.getRemark());
                    requestDto.setProcessVariables(processVariables);
                    requestDto.setBizKey(claimId.toString());
                    requestDto.setUid(user.getUserNum());
                    requestDto.setUserId(userId);

                    WorkflowSubmitAndAgreeResult submitResult  =bpmApiService.submit(requestDto);
//                    processCreateFacadeService.execute(mo2);
//                    pendingId=(String) mo2.getInput("nextWorkitemId");
//                    nextActivityInstID=(String) mo2.getInput("nextActivityInstID");

                    pendingId =submitResult.getProcessDefId();
//                    nextActivityInstID=(String) mo2.getInput("nextActivityInstID");
                    code="0";
                    success=true;
                    massage="创建流程成功！";
                   log.info("报账单"+claim.getClaimNo()+"财务共享app端流程发送成功");
                } catch (Exception e) {
                    code="1";
                    success=false;
                    massage="创建流程失败！";
                    log.error("报账单"+claim.getClaimNo()+"财务共享app端流程发送失败",e);
                    throw new BusinessException(e);
                }
            }
        }else{
//            TProcessWiparticipantRecordQueueDAOImpl tprocessWiparticipantRecordQueueDAO = (TProcessWiparticipantRecordQueueDAOImpl) AppBeanFactory.getBean("tprocessWiparticipantRecordQueueDAO");
//            String hql = "from TProcessWiparticipantRecord where claimId=?  and currentState='10'  order by workitemId desc";
//            List<TProcessWiparticipantRecord> recordList = tprocessWiparticipantRecordQueueDAO.find(hql, new Object[]{claim.getClaimId()});
            List<TProcessWiRecord>  recordList =tProcessWiRecordRepository.findByClaimIdAndCurrentStateOrderByWorkItemIdDesc(claim.getClaimId(),10);
            if(recordList != null && recordList.size()>0){
                pendingId = String.valueOf(recordList.get(0).getId());
            }
        }
//        处理特殊字符
        if(massage != null && !"".equals(massage)){
            massage = massage.replaceAll("●", "");
            massage = massage.replaceAll("【", ":");
            massage = massage.replaceAll("】", "");
        }else{
            massage = "报账单发送成功";
            if("109".equals(code)){
                massage = "发送失败！";
            }
        }

        Map<String,Object> result = new HashMap<>();
        result.put("pendingId", pendingId);
        result.put("nextActivityInstID", nextActivityInstID);
       result.put("code", code);
       result.put("message", massage);
       result.put("result", result);
       result.put("success", success);


        return Result.success(result);
    }

    /**
     * 封装code 110 的返回数据
     */
    public Result handleCode110Data( AppError err)  {
        Map<String,Object> result = new HashMap<>();
        List<TRmbsDict> dictList = rmbsDictDao.getByDictId("verifInstructions");
        TRmbsDict dictVo = new TRmbsDict();
        dictVo.setDictId("other");
        dictVo.setDictName("其他");
        dictList.add(0, dictVo);
        result.put("writedList", dictList);
       result.put("code", "110");
       result.put("message", err.getCode());
       result.put("result", result);
       result.put("success", false);
        return Result.failure(result);
    }

    /**
     * 删除单据
     *
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteClaim(DeleteClaimParam param, UserObject user) {

        try {
            log.warn("start 删除单据{}",param);
//            String itemId = mo.getInput("itemId").toString();
//            String claimId = mo.getInput("claimId").toString();
//            mo.setInput(ClaimServiceBase.REQ_DELETE_IDS, claimId);
//            mo.setInput("itemId", itemId);
//            mo.setInput("compId", mo.getUser().getCurCompId());
//            mo.setInput("processStateEng", "rootDrafterActivity");
//            String claimTypeName = "delete" + itemId + "ClaimService-gbs";
//            DeleteClaimService deleteClaimService = (DeleteClaimService) AppBeanFactory.getBean(claimTypeName);
//            deleteClaimService.execute(mo);
            this.deleteClaimService.delete(param.getClaimId(), user);
//            map.put("message", "删除成功");
//            map.put("status", "S");
//            map.put("code", "0");
            log.warn("end 删除单据成功{}",param);
        } catch (Exception e) {
            log.error("删除错误",e);
            //手工回滚异常
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw new BusinessException(e);
        }

        return true;
    }

    /**
     * 智能报账保存
     *
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TRmbsClaim saveOrSendClaim(SaveOrSendClaimParam param, UserObject user) {

        JSONObject getjsonParamByRequest = JSONObject.parseObject(param.getDataStr());
        Map<String, Object> map = new HashMap<>();

        String REGEX_CHINESE = "[\u4e00-\u9fa5]";// 中文正则
        Pattern pat = Pattern.compile(REGEX_CHINESE);
        TRmbsClaim claim = new TRmbsClaim();

        try {
            if(param.getClaimId()!=null&&param.getClaimId()!=0L){
                deleteClaim(new DeleteClaimParam(param.getClaimId()),user);
            }
            JSONArray baseData = (JSONArray) getjsonParamByRequest.get("baseData");
            JSONObject claimInfoVo = (JSONObject) getjsonParamByRequest.get("claimInfoVo");
            JSONArray lineList = (JSONArray) getjsonParamByRequest.get("lineList");
            JSONArray relList = (JSONArray) getjsonParamByRequest.get("relList");
            JSONArray invoiceList= (JSONArray) getjsonParamByRequest.get("data");
            String invoiceIds = "";
            List<String> invoiceIdList = new ArrayList<>();
            for (Object invoiceObj : invoiceList) {
                invoiceIdList.add(((JSONObject)invoiceObj).get("id").toString());
            }
            invoiceIds = Joiner.on(",").join(invoiceIdList);
            List<String> folderTreeList = iAppInvoiceDao.getFolderTreeList(invoiceIds, null);
            invoiceIds =  Joiner.on(",").join(folderTreeList.toArray());

            String imageIds = "";
            if(getjsonParamByRequest != null &&  getjsonParamByRequest.containsKey("invoice")){


            }
            String itemId = claimInfoVo.get("itemId").toString();
            String item2Id = claimInfoVo.get("item2Id").toString();
            String payAmount = "";
            String subtractionSumAmount = claimInfoVo.get("subtractionSumAmount") == null ? "" : claimInfoVo.get("subtractionSumAmount").toString();
            String verifInstructions = claimInfoVo.get("verifInstructions") == null ? "" : claimInfoVo.get("verifInstructions").toString();
            String verifInstructionsCode = claimInfoVo.get("verifInstructionsCode") == null ? "" : claimInfoVo.get("verifInstructionsCode").toString();
            String imageMountMode = claimInfoVo.get("imageMountMode") == null ? "PD-2": claimInfoVo.get("imageMountMode").toString();

            String orgId = "";
            String buSegCode = "";
            String buSeg = "";
            String expenseIssuerId = "";
            String projectName = "不分明细";
            String projectNum = "0";
            String projectId = "";
            for (int i = 0; i < baseData.size(); i++) {
                JSONObject jsonObject = baseData.getJSONObject(i);
                if (jsonObject.get("valueKey").equals("orgId")) {
                    orgId = jsonObject.get("value").toString();
                }
                if (jsonObject.get("valueKey").equals("buSegCode")) {
                    buSegCode = jsonObject.get("value").toString();
                    buSeg = jsonObject.get("text").toString();
                }
                if (jsonObject.get("valueKey").equals("applyUserName")) {
                    expenseIssuerId = jsonObject.get("value").toString();
                }
                if (jsonObject.get("valueKey").equals("payAmount")) {
                    payAmount = jsonObject.get("value").toString();
                }
                if (jsonObject.get("valueKey").equals("projectNum")) {
                    projectNum = jsonObject.get("text").toString();
                }
                if (jsonObject.get("valueKey").equals("projectName")) {
                    projectName = jsonObject.get("text").toString();
                }
            }

            String item2Name = level2Repository.findByItemIdAndNewStateAndOrgId(item2Id,1,Integer.valueOf(orgId)).get(0).getItemName();
            if (claimInfoVo.containsKey("projectId")) {
                projectId = claimInfoVo.get("projectId").toString();
            }

            Map inputMap =  BeanUtil.beanToMap(baseClaimService.initNewClaim(itemId,user),false,true);
            inputMap.putAll(BeanUtil.beanToMap(claimInfoVo,false,true));

            inputMap.put("orgId", orgId);
            inputMap.put("buSeg", buSeg);
            inputMap.put("buSegCode", buSegCode);
            inputMap.put("itemId", itemId);
            inputMap.put("item2Id", item2Id);
            inputMap.put("item2Name", item2Name);
            inputMap.put("expenseIssuerId", expenseIssuerId);
            inputMap.put("projectId", projectId);
            inputMap.put("projectName", projectName);
            inputMap.put("projectNum", projectNum);
            inputMap.put("subtractionSumAmount", subtractionSumAmount);


            if(projectNum != null && !"".equals(projectNum) && !"0".equals(projectNum)){

                PmProject pmProject = pmProjectRepository.findByProjectNo(projectNum);
                if(pmProject == null){
                    throw  new Exception( "●请选择在建工程项目编码及名称！");
                }else if(!"2".equals(pmProject.getProjectTypeCode())){
                    throw  new Exception( "●请选择在建工程项目编码及名称！");
                }
            }

            List<OcrCallbackOcrinvoice> list = iAppInvoiceDao.findBySql("select * from ocr_callback_ocrinvoice a   where a.APP_OCR_IMAGE_ID in ( select f.OCR_IMAGE_ID from t_app_invoice_folder f where id in ( " + invoiceIds + ") ) ",new HashMap<>(),OcrCallbackOcrinvoice.class);

            if(list != null && list.size() > 0){
                for (OcrCallbackOcrinvoice ocrinvoice : list) {
                    if (!itemId.equals("T007") && Arrays.asList("13,14,15,16,21,22".split(",")).contains(ocrinvoice.getInvoiceTypeModify())) {
                        throw new Exception("此报账单不支持国内旅客运输发票的智能填单，请使用手工填写模式，发票类型选择：普通发票/电子发票。");
                    }
                }
            }

            if (itemId.equals("T019") || itemId.equals("T017")) {


            } else {
                TCoComsegcode coCom = coComSegCodeDao.getTCoComsegcodeGroup_t001(user.getCurCompId(), Long.parseLong(orgId), user.getCurCostCenterId(), user.getCurGroupId(), user.getCurCostCenterName());
                String groupAttributeId = "0";
                if (coCom != null) {
                    groupAttributeId = coCom.getGroupAttributeId();

                    //OU信息
                    inputMap.put("orgId", coCom.getOrgId().toString());//OU ID
                    inputMap.put("orgName", coCom.getOrgName());//ou 名称

                    inputMap.put("coSegCode", coCom.getCoSegCode());//ID
                    inputMap.put("coSeg", coCom.getCoSegName());//名称

                    inputMap.put("setOfBooksId", coCom.getSetOfBooksId());//ID
                    inputMap.put("setOfBooks", coCom.getSetOfBooksName());//名称

                } else {
                    throw new BusinessException("费用承担部门与报账平台配置的对应费用承担部门不一致，请联系业务财务确认！");
                }

                if (groupAttributeId != null && !"".equals(groupAttributeId)) {
                    TCoItem2Deptcost cost = coItem2DeptcostDao.getTCoItem2DeptCost(item2Id, groupAttributeId);
                    if (cost != null) {
                        inputMap.put("item2Name", cost.getItemName());//大类名称
                        inputMap.put("flexCode", cost.getCashFlow());//现金流量标识
                        inputMap.put("flexName", cost.getDescription());//现金流量标识名称
                        inputMap.put("groupAttributeId", groupAttributeId);//部门属性

                    }
                }
            }

            inputMap.put("verifInstructions", verifInstructions);
            inputMap.put("verifInstructionsCode", verifInstructionsCode);

            inputMap.put("imageMountMode", imageMountMode);
            inputMap.put("subtractionSumAmount", subtractionSumAmount);
            if(imageIds.equals("")){

                List<TAppInvoiceFolder> tAppInvoiceFolderGroupList = iAppInvoiceDao.getTAppInvoiceFolderGroupList(invoiceIds);
                Set<String> imageIdSet = new HashSet<>();
                if (tAppInvoiceFolderGroupList != null && tAppInvoiceFolderGroupList.size() > 0) {
                    for (TAppInvoiceFolder tAppInvoiceFolder : tAppInvoiceFolderGroupList) {
                        if (tAppInvoiceFolder.getClaimNo() != null) {
                            throw new Exception("发票已经被报账单:" + tAppInvoiceFolder.getClaimNo() + "关联使用！");
                        }
                        imageIdSet.add(tAppInvoiceFolder.getImageId().toString());
                    }
                }
                String[] imageIdArr = new String[imageIdSet.size()];
                imageIdSet.toArray(imageIdArr);
                imageIds = Joiner.on(",").join(imageIdArr);
            }
            List<Map> maps = intellijFillClaimService.ocrCallBackInvoiceToTaxLine(null, imageIds, itemId, item2Id, claim);

            BigDecimal foreignApplyAmountSum1 = BigDecimal.ZERO;
            for (int i = 0; i < lineList.size(); i++) {
                JSONObject jsonObject = lineList.getJSONObject(i);
                String foreignApplyAmount = jsonObject.get("foreignApplyAmount").toString();
                foreignApplyAmountSum1 = foreignApplyAmountSum1.add(new BigDecimal(foreignApplyAmount));
            }
            BigDecimal foreignApplyAmountSum2 = BigDecimal.ZERO;
            for (Map map1 : maps) {
                if (!map1.get("lineAmount").toString().equals("")) {
                    BigDecimal lineAmount = new BigDecimal(map1.get("lineAmount").toString());
                    foreignApplyAmountSum2 = foreignApplyAmountSum2.add(lineAmount);
                }
            }
            if(Arrays.asList(TAppInvoiceFolder.specialT019).contains(item2Id)){
                Map mapt019 = intellijFillClaimService.calculateClaimAmountForT019(maps);
                subtractionSumAmount =  mapt019.get("subtractionSumAmount").toString();
                inputMap.put("subtractionSumAmount", subtractionSumAmount);
            }

           ClaimPageParams claimPageParams = baseClaimService.saveClaim (OrikaBeanUtil.map(inputMap, ClaimPageParams.class),user);
            claim = ClaimPageParams.transferClaimPageParams2Entity(claimPageParams);
            TRmbsClaimLock tRmbsClaimLock =claimLockService.saveLockClaim(claim.getClaimId());

          /*  if(foreignApplyAmountSum1.compareTo(foreignApplyAmountSum2)!=0){
                throw  new Exception("发票金额 与 明细金额不一致");
            }
         */
            //借款核销保存
            StringBuilder insertIds = new StringBuilder();
            if (relList != null && relList.size() > 0) {
                for (int i = 0; i < relList.size(); i++) {
                    JSONObject jsonObject = relList.getJSONObject(i);

                    Object writedMoney = jsonObject.get("writedMoney");
                    if (writedMoney==null) {
                        writedMoney = jsonObject.get("borrowMonkey");
                    }
                    insertIds.append(writedMoney.toString())//本次核销金额
                            .append(",")
                            .append(jsonObject.get("claimIdRel").toString())
                            .append(";");
                }
                Map subDataMap = new HashMap();
                subDataMap.putAll(inputMap);
                subDataMap.put("userId", user.getUserId());
                subDataMap.put("claimVo", claim);
                subDataMap.put("insertIds", insertIds.toString());
                saveClaimRelSave(user, subDataMap,claim);
            }
            inputMap.put("claimId", claim.getClaimId().toString());
            inputMap.put("imageIds", imageIds);
            inputMap.put("subtractionSumAmountVal", subtractionSumAmount != null && subtractionSumAmount.trim().length() > 0 ? subtractionSumAmount : "0.00");
            intellijFillClaimService.saveClaimTaxSave(claim.getClaimId(),imageIds,claim,user);

            //明细航保存
            for (int i = 0; i < lineList.size(); i++) {
                JSONObject jsonObject = lineList.getJSONObject(i);
                Map subDataMap = new HashMap();
                subDataMap.putAll(inputMap);
                subDataMap.putAll(jsonObject);
                subDataMap.put("userId", user.getUserId());
                subDataMap.put("claimVo", claim);
                subDataMap.put("saveFlag", "PD-1");
                subDataMap.put("assAppCreateFlagId", imageIds);
                intellijFillClaimService. saveClaimLine(user, subDataMap,claim);
            }

            Map rsMap = new HashMap();
            Timestamp nowTime = new Timestamp(System.currentTimeMillis());
            rsMap.put("claimId", claim.getClaimId());
            rsMap.put("lockId", tRmbsClaimLock.getId());
            rsMap.put("lockTime", tRmbsClaimLock.getSubmitDate().toString());

            map.put("result", rsMap);
            map.put("message", "保存成功");
            map.put("code", "0");
            map.put("success", "true");

            intellijFillClaimService.setAppInvoiceAssData(imageIds, claim.getClaimNo(), claim.getClaimId(), "appToClaim");

        } catch (Exception e) {
            log.error("错误",e);
            //手工回滚异常
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw new BusinessException(e);
        }
        return claim;
    }

    public void saveClaimRelSave(UserObject user, Map dataMap, TRmbsClaim claim) throws Exception {

        try {

            dataMap.put("claimId", claim.getClaimId());
            dataMap.put("itemId", claim.getItemId());

            ClaimRelReqParams claimRelReqParams = OrikaBeanUtil.map(dataMap,ClaimRelReqParams.class);
            ClaimRelPageParams claimRelPageParams = claimRelService.initNewRel(claimRelReqParams,user);
            claimRelService.saveRel(claimRelPageParams, user);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }
    }

    /**
     * 获取下一环节
     *
     */
    @Override
    public Map<String,Object> appGetNextSelect(AppGetNextSelectParam param, UserObject user) {
        String claimId = param.getClaimId().toString();
        String pendingId =param.getPendingId();
        String nextActivityInstID = param.getNextActivityInstID();
        String hrNo = param.getHrNo();
        String lockId = param.getLockId();
        String lockTime = param.getLockTime();
        String processStateEng=param.getProcessStateEng();//页面当前流程状态
        String comment = param.getComment();
        if(comment == null || "".equals(comment)){
            comment = "同意";
        }
//		if(!isUTFEncoding()){
//			comment = new String(comment.getBytes("ISO-8859-1"), "UTF-8");
//		}
        try{
            //========加密验证 start==========
//            String resultString = CodeUtil.setMessageObject(getRequest(),getResponse(),configUtil.getBaseUrl(),"1");
//            if("1".equals(resultString)){
//                throw new Exception("加密验证失败！请刷新界面！");
//            }
//            //========加密验证 end==========
//            LOGGER.info("getNextSelectApp REQUEST: " + configUtil.getBaseUrl()
//                    + "mobileQueryNextActivityServlet?claimId=" + claimId + "&pendingId=" + pendingId + "&hrNo=" + hrNo
//                    + "&nextActivityInstID=" + nextActivityInstID+"&processStateEng="+processStateEng);
//
            List<MobileNextActivityVo> nextActivityVoList = mobileQueryNextActivityServlet(param,user);
//            LOGGER.info("getNextSelectApp :" + URLDecoder.decode(s,"UTF-8"));
//            if(s.indexOf("error")>0){
//                JSONObject jsonObject =JSONObject.parseObject(s);
//                String str = "{\"code\":1,\"result\":null,\"message\":\""+jsonObject.get("error")+"\",\"success\":false}";
//                writeString(str);
//            }else{
//            String s =valiTaxTransferController(claimId);
//                JSONArray jsonObject = JSONArray.parseArray(s);
//                List<MobileNextActivityVo> list = new ArrayList<MobileNextActivityVo>();
//                for(int i=0; i<jsonObject.size();i++){
//
//                    JSONObject object = jsonObject.getJSONObject(i);
//                    JSONArray jsonParticipant = JSONArray.parseArray(JSONObject.toJSONString(object.get("nextParticipant")));
//                    List<MobileParticipantVo> listParticipant = new ArrayList<MobileParticipantVo>();
//                    if(jsonParticipant != null){
//                        for(int j=0; j<jsonParticipant.size();j++){
//                            JSONObject objectParticipant = jsonParticipant.getJSONObject(j);
//                            listParticipant.add((MobileParticipantVo)JSONObject.toJavaObject(objectParticipant, MobileParticipantVo.class));
//                        }
//                    }
//                    MobileNextActivityVo mobileNextActivityVo = (MobileNextActivityVo)JSONObject.toJavaObject(object, MobileNextActivityVo.class);
//                    mobileNextActivityVo.setNextParticipant(listParticipant);
//                    list.add(mobileNextActivityVo);
////                }

                //		getRequest().setAttribute("claimId", getRequest().getParameter("claimId"));
                //		getRequest().setAttribute("pendingId", getRequest().getParameter("pendingId"));
                //		getRequest().setAttribute("comment", comment);
                //		getRequest().setAttribute("nextActivityVoList", list);
                Map<String,Object> map = new HashMap<>();
                map.put("claimId", claimId);//0：成功
                map.put("pendingId", pendingId);//查询结果
                map.put("comment", comment);//信息
                map.put("lockId", lockId);//流程实例锁id
                map.put("lockTime", lockTime);//流程实例锁时间
                map.put("nextActivityVoList", nextActivityVoList);//成功标志
                Map<String,Object> resultmap = new HashMap<>();
                resultmap.put("code", 0);//0：成功
                resultmap.put("result", map);//查询结果
                resultmap.put("message", "查询审核环节成功");//信息
                resultmap.put("success", true);//成功标志
//                String str = JSONObject.toJSONString(resultmap,SerializerFeature.WriteMapNullValue);
                // 下一环节只有一个审批人
                if(nextActivityVoList.size() == 1){
//                    MobileNextActivityVo mobileNextActivityVo = list.get(0);
//                    if(!"灵活审批".equals(mobileNextActivityVo.getNextActivityName()) && mobileNextActivityVo.getNextParticipant().size() == 1){
//                        MobileParticipantVo mobileParticipantVo = mobileNextActivityVo.getNextParticipant().get(0);
//                        appSingleApprove(pendingId, mobileParticipantVo.getUserId()+"-"+mobileParticipantVo.getGroupId(), claimId, comment, mobileNextActivityVo.getNextActivityId(),lockId,lockTime);
//                    }else{
//                        writeString(str);
//                    }
                    //发送流程 TODO
                    return resultmap;
                }else{
//                    writeString(str);
                    return resultmap;
                }
//            }
        } catch (Exception e) {
            throw new BusinessException(e);
//            String str = "{\"code\":1,\"result\":null,\"message\":\"获取下一环节出错\",\"success\":false}";
//            LOGGER.info("mobileQueryNextActivityServlet :" + str);
//            getRequest().setAttribute("object", str);
//            writeString(str);
//            e.printStackTrace();
        }
    }

    private List<MobileNextActivityVo> mobileQueryNextActivityServlet(AppGetNextSelectParam param, UserObject user) {

        String userHrNo = param.getHrNo();
        Long claimId = param.getClaimId();
        String pendingId = param.getPendingId();
        String nextActivityInstID=param.getNextActivityInstID();
        String processStateEng=param.getProcessStateEng();
        List<MobileNextActivityVo> mobileNextActivityVoList = new ArrayList();
        try {
            valiTaxTransferController(claimId);
//            TProcessWiparticipantRecordQueueDAOImpl tprocessWiparticipantRecordQueueDAO = (TProcessWiparticipantRecordQueueDAOImpl) AppBeanFactory.getBean("tprocessWiparticipantRecordQueueDAO");
//            ProcessInstControllerImpl processInstControllerImpl = (ProcessInstControllerImpl) AppBeanFactory.getBean("processInstControllerImpl");
//            IBPSServiceClient BPSclient = BPSServiceClientFactory.getDefaultClient();
//            String serverAddress = ServiceAddressUtil.getInstance().getServerAddress();

            TRmbsClaim claim = rmbsClaimRepository.findByClaimId(claimId);
            TProcessWiRecord record = tProcessWiRecordRepository.findById( Long.parseLong(pendingId)).orElse(null);

            SysUser sysUser = sysUserRepository.findByUserName(userHrNo);
            if (sysUser==null ){
                throw new Exception("当前待办人员不存在 编号："+userHrNo);
            }
            if(processStateEng!=null&&!processStateEng.equals("")&&!processStateEng.equals("null")){
                if(!claim.getProcessStateEng().equals(processStateEng)){
                    if(claim.getProcessStateEng().equals("drafterActivity")){
                        if(!processStateEng.equals("financePreReviewActivity")&&!processStateEng.equals("financeDoubleCheckActivity")
                                &&!processStateEng.equals("financeManagerActivity")&&!processStateEng.equals("financeReviewActivity")){
                            throw new Exception("该报账单已经由起草人撤回");
                        }
                    }
                }
            }

//            List<WFActivityDefine> nextActlist=null;
//            if(claim.getProcessStateEng().equals("rootDrafterActivity")){
//
//                ContextDto contextDto = new ContextDto();
//                contextDto.put(ContextDto.OLD_TASKINST_ID,pendingId);
//                contextDto.put(ContextDto.OLD_ACTIVITYDEF_ID, "rootDrafterActivity");
//                contextDto.put(ContextDto.SERVER_ADDRESS, serverAddress);
//                contextDto.put(ContextDto.CUR_USER_ID, suList.get(0).getUserid().toString());
//                contextDto.put(ContextDto.CUR_USER_NAME, suList.get(0).getUsername());
//                contextDto.put(ContextDto.CUR_GROUP_ID, String.valueOf(claim.getApplyDeptId()));
//                contextDto.put(ContextDto.CUR_GROUP_NAME, claim.getApplyDeptName());
//                contextDto.put(ContextDto.BIZ_STATE, String.valueOf(TProcessWiparticipantRecord.BIZSTATE_ROOT));
//                nextActlist = BPSclient.getProcessInstManager().getNextActivitiesMaybeArrived(Long.valueOf(nextActivityInstID));
//                CommonUtil.printlnLog(claimId, "手机审批 查询后续环节  2  结束");
//            }else{
//
//                if (record.getCurrentState().longValue() != 10){
//                    throw new Exception("当前不是待办状态  状态为："+record.getCurrentState());
//                }
//
//                if (!record.getHandlerUserid().equals(suList.get(0).getUserid().toString())){
//                    throw new Exception("当前操作人员与待办人不一致 操作人员："+suList.get(0).getUserid().toString()+"  待办人员："+record.getHandlerUserid());
//                }
//
//                ContextDto contextDto = new ContextDto();
//                contextDto.put(ContextDto.OLD_TASKINST_ID, record.getWorkitemId().toString());
//                contextDto.put(ContextDto.OLD_ACTIVITYDEF_ID, record.getActivityDefId());
//                contextDto.put(ContextDto.SERVER_ADDRESS, serverAddress);
//                contextDto.put(ContextDto.CUR_USER_ID, suList.get(0).getUserid().toString());
//                contextDto.put(ContextDto.CUR_USER_NAME, suList.get(0).getUsername());
//                contextDto.put(ContextDto.CUR_GROUP_ID, record.getHandlerOrgid().toString());
//                contextDto.put(ContextDto.CUR_GROUP_NAME, record.getHandlerOrgName());
//                contextDto.put(ContextDto.BIZ_STATE, String.valueOf(TProcessWiparticipantRecord.BIZSTATE_EXECUTE));
//
//                CommonUtil.printlnLog(claimId, "手机审批 查询后续环节  1  流程设置相关数据区 开始");
//                processInstControllerImpl.setRelativeData(record.getProcessInstId().toString(), contextDto.getTransMap());
//                CommonUtil.printlnLog(claimId, "手机审批 查询后续环节  1  流程设置相关数据区 结束");
//
//                CommonUtil.printlnLog(claimId, "手机审批 查询后续环节  2  开始");
//                nextActlist = BPSclient.getProcessInstManager().getNextActivitiesMaybeArrived(record.getActivityInstId());
//                CommonUtil.printlnLog(claimId, "手机审批 查询后续环节  2  结束");
//            }

//            if (nextActlist!=null && nextActlist.size()>0){
//                for (int i=0 ; i<nextActlist.size() ; i++){
//                    WFActivityDefine activityDefine = nextActlist.get(i);
//                    if (activityDefine.getName().startsWith("灵活审批")){
//                        CommonUtil.printlnLog(claimId, "手机审批 查询后续环节  3  "+activityDefine.getId()+"环节  灵活审批");
//                        MobileNextActivityVo vo = new MobileNextActivityVo();
//                        vo.setPendingId(pendingId);
//                        vo.setClaimId(claimId);
//                        vo.setNextActivityId(activityDefine.getId());
//                        vo.setNextActivityName(activityDefine.getName());
//                        mobileNextActivityVoList.add(vo);
//                        continue;
//                    } else {
//                        CommonUtil.printlnLog(claimId, "手机审批 查询后续环节  3  查询"+activityDefine.getId()+"环节 参与者  开始");
//                        List<WFParticipant> participantlist=null;
//                        if(claim.getProcessStateEng().equals("rootDrafterActivity")){
//                            participantlist = BPSServiceClientFactoryExt.getDefaultClient().getService(IBPSServiceCustExt.class).getProbableParticipantsExt(claim.getProcessInstanceId(), Long.valueOf(pendingId), activityDefine.getId(), "0");
//                        }else{
//                            participantlist = BPSServiceClientFactoryExt.getDefaultClient().getService(IBPSServiceCustExt.class).getProbableParticipantsExt(record.getProcessInstId(), record.getWorkitemId(), activityDefine.getId(), "0");
//                        }
//                        CommonUtil.printlnLog(claimId, "手机审批 查询后续环节  3  查询"+activityDefine.getId()+"环节 参与者  结束");
//                        MobileNextActivityVo vo = new MobileNextActivityVo();
//                        vo.setPendingId(pendingId);
//                        vo.setClaimId(claimId);
//                        vo.setNextActivityId(activityDefine.getId());
//                        vo.setNextActivityName(activityDefine.getName());
//                        if (participantlist!=null && participantlist.size()>0){
//                            List nextParticipantVoList = new ArrayList();
//                            for (int j=0 ; j<participantlist.size() ; j++){
//                                WFParticipant participant = participantlist.get(j);
//                                if (participant.getTypeCode().equals("person")){
//                                    MobileParticipantVo participantVo = new MobileParticipantVo();
//                                    participantVo.setFullname(participant.getName());
//                                    participantVo.setUserId(participant.getId());
//                                    SysUser user = tprocessWiparticipantRecordQueueDAO.get(SysUser.class, Long.parseLong(participant.getId()));
//                                    participantVo.setUsername(user.getUsername());
//                                    if (participant.getAttribute("parentid") != null){
//                                        participantVo.setGroupId(participant.getAttribute("parentid").toString());
//                                        SysGroup sysGroup = tprocessWiparticipantRecordQueueDAO.get(SysGroup.class, Long.parseLong(participant.getAttribute("parentid").toString()));
//                                        participantVo.setGroupName(sysGroup.getGroupName());
//                                    }
//                                    nextParticipantVoList.add(participantVo);
//                                }
//                            }
//                            vo.setNextParticipant(nextParticipantVoList);
//                        }
//                        mobileNextActivityVoList.add(vo);
//                    }
//                }
//                com.alibaba.fastjson.JSONArray jsonArray = (com.alibaba.fastjson.JSONArray) JSON.toJSON(mobileNextActivityVoList);
//                out.print(jsonArray.toJSONString());
//                CommonUtil.printlnLog(claimId, "手机审批 查询后续环节 json:"+jsonArray.toJSONString());
//            } else {
//                JSONObject errorJson = JSON.parseObject("{\"error\":\"查询报错 后续环节为空 "+claimId+"  pendingId="+pendingId+"\"}");
//                CommonUtil.printlnLog(claimId, "手机审批 查询后续环节 查询报错 后续环节为空  errJson:"+errorJson.toJSONString());
//                out.print(errorJson.toJSONString());
//            }
        } catch (Exception e){

            log.error(claimId+"手机审批 查询后续环节 err : "+e.getMessage(),e);
            throw new BusinessException(e);
        }
        return mobileNextActivityVoList;
    }

    public void valiTaxTransferController(Long claimId) throws Exception {

//
//        ITRmbsClaimDao claimDao = (ITRmbsClaimDao) AppBeanFactory.getBean("claimDao");
//        TRmbsClaimLineDaoImpl rmbsClaimLineDao  = (TRmbsClaimLineDaoImpl)AppBeanFactory.getBean("rmbsClaimLineDao");
//        String claimSql=" from TRmbsClaim where claimId = "+claimId;
//        List<TRmbsClaim> RmbsClaimList=claimDao.find(claimSql);
        TRmbsClaim claim = rmbsClaimRepository.findByClaimId(claimId);
        String itemId=claim.getItemId();

        if((itemId.equals("T007")||itemId.equals("T008"))){
            String isFullTransferItem3Id="";
            String TransferControlItem3Id="";


            List<TRmbsTaxTransferControl>list=rmbsTaxTransferControlRepository.findAll();
            if(list!=null&& list.size()>0){
                try{
                    TRmbsTaxTransferControl tRmbsTaxAutoTransferControl = list.get(0);
                    isFullTransferItem3Id=tRmbsTaxAutoTransferControl.getIsFullTransferItem3Id();
                    TransferControlItem3Id=tRmbsTaxAutoTransferControl.getTransferControlItem3Id();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }

            List<TRmbsClaimLine> tRmbsClaimLineList=claimLineRepository.findByClaimId(claimId);
            Map<String, BigDecimal> segmentSumAmount = new HashMap<>();

            for (TRmbsClaimLine tRmbsClaimLine : tRmbsClaimLineList) {

                if (claim.getProcessStateEng().equals("bizAccountActivity")) {
                    String item3Id = tRmbsClaimLine.getItem3Id();
                    if (!Arrays.asList(isFullTransferItem3Id.split(",")).contains(item3Id)
                            && Arrays.asList(TransferControlItem3Id.split(",")).contains(item3Id)
                    ) {
                        throw new Exception("●本报账单包含需要进项税转出的业务小类，请在电脑端进行操作！");
                    }
                }
            }
        }
    }
}
