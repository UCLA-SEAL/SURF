package org.jivesoftware.openfire.plugin.conferenceVote;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.QName;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.plugin.conferenceVote.model.ConferenceOptionBean;
import org.jivesoftware.openfire.plugin.conferenceVote.model.ConferenceSelectBean;
import org.jivesoftware.openfire.plugin.conferenceVote.model.ConferenceVoteBean;
import org.jivesoftware.openfire.plugin.conferenceVote.util.Constants;
import org.jivesoftware.openfire.plugin.conferenceVote.util.DateUtil;
import org.jivesoftware.openfire.plugin.conferenceVote.util.NumberUtil;
import org.xmpp.packet.IQ;

public class LoadVoteAction extends BaseAction {

	@Override
	public List execute(IQ request) {
		Connection con = null;
        PreparedStatement pstmt = null;
        boolean abortTransaction = false;
        ResultSet rs = null;
        ConferenceVoteBean voteBean = new ConferenceVoteBean();
        List replys = new ArrayList();
        int total_num = 0;
		try {
			con = DbConnectionManager.getConnection();
			parseXML(request,con,voteBean);
			List<ConferenceVoteBean> voteList = new ArrayList<ConferenceVoteBean>();
			Map<Integer,ConferenceVoteBean> voteMap = new HashMap<Integer,ConferenceVoteBean>();
			Map<Integer,ConferenceSelectBean> selectMap = new HashMap<Integer,ConferenceSelectBean>();
//			pstmt = con.prepareStatement("select cv.begin_time,cv.id vid,cv.title_remark,cv.title,cv.effective_time," +
//					"cv.is_close,cv.is_start,cs.id sid,cs.type,cs.select_title,cs.select_remark,cs.select_num," +
//					"co.id oid,co.option_name,su.lcc_account from conference_vote cv,conference_select cs," +
//					"conference_option co,sys_user su where cv.conference_id=? "+ 
//			"and cv.id=cs.conference_vote_id and cs.id=co.conference_select_id and su.user_id=cv.creator");
			pstmt = con.prepareStatement("select cv.begin_time,cv.id vid,cv.title_remark,cv.title,cv.effective_time," +
					"cv.is_close,cv.is_start,cs.id sid,cs.type,cs.select_title,cs.select_remark,cs.select_num," +
					"co.id oid,co.option_name,su.lcc_account from conference_vote cv left outer join conference_select cs on cv.id=cs.conference_vote_id " +
					"left outer join conference_option co on cs.id=co.conference_select_id,sys_user su where cv.conference_id=? "+ 
			" and su.user_id=cv.creator order by cv.id desc");
            pstmt.setInt(1, voteBean.getConferenceId());
            rs = pstmt.executeQuery();
            while(rs.next()){
            	Integer vid = new Integer(rs.getInt("vid"));
            	ConferenceVoteBean vb = null;
            	ConferenceSelectBean sb = null;
            	if(!voteMap.containsKey(vid)){
            		vb = new ConferenceVoteBean();
            		vb.setId(vid);
            		vb.setTitle(rs.getString("title"));
            		vb.setTitleRemark(rs.getString("title_remark"));
            		vb.setEffectiveTime(rs.getInt("effective_time"));
            		vb.setConferenceId(voteBean.getConferenceId());
            		vb.setCreatorStr(rs.getString("lcc_account"));
            		vb.setClose(rs.getBoolean("is_close"));
            		vb.setStart(rs.getBoolean("is_start"));
            		vb.setBeginTime(rs.getTimestamp("begin_time"));
            		voteMap.put(vid, vb);
            	}else{
            		vb = voteMap.get(vid);
            	}
            	Integer sid = new Integer(rs.getInt("sid"));
            	if(sid!=null && sid>0){
            		if(!selectMap.containsKey(sid)){
                		sb = new ConferenceSelectBean();
                		sb.setId(sid);
                		sb.setConferenceVoteId(vid);
                		sb.setType(rs.getInt("type"));
                		sb.setSelectTitle(rs.getString("select_title"));
                		sb.setSelectRemark(rs.getString("select_remark"));
                		sb.setSelectNum(rs.getInt("select_num"));
                		selectMap.put(sid, sb);
                		vb.getSelects().add(sb);
                	}else{
                		sb = selectMap.get(sid);
                	}
            		Integer oid = new Integer(rs.getInt("oid"));
                	ConferenceOptionBean ob = new ConferenceOptionBean();
                	ob.setId(oid);
                	ob.setConferenceSelectId(sid);
                	ob.setOptionName(rs.getString("option_name"));
                	sb.getOptions().add(ob);
            	}
            	
            	
            }
//            DbConnectionManager.fastcloseStmt(pstmt);
            Iterator iter = voteMap.entrySet().iterator(); 
            while (iter.hasNext()) { 
                Map.Entry entry = (Map.Entry) iter.next(); 
//                Object key = entry.getKey(); 
                ConferenceVoteBean val = (ConferenceVoteBean)entry.getValue();
                voteList.add(val);
            } 
            replys.add(this.getSuccessReplyIQ(request, voteList, voteBean));
//            List<String> userAddress = this.getUserAddress(voteBean.getRoomJid());
//            replys.add(userAddress);
        }
        catch (Exception e) {
        	e.printStackTrace();
            abortTransaction = true;
            
            replys.add(this.getFailReplyIQ(request, voteBean));
        }
        finally {
            DbConnectionManager.closeConnection(rs, pstmt, con);
        }
		return replys;
	}

	
	public IQ getFailReplyIQ(IQ request,ConferenceVoteBean voteBean){
		IQ reply = IQ.createResultIQ(request);
        Element queryResult = DocumentHelper.createElement(QName.get(Constants.PREFIX_NAME, Constants.SPACE_NAME));
        Element operate = queryResult.addElement("operate");
        operate.addAttribute("name", ActionFactory.LOAD_VOTE);
        operate.addElement("result").addText(Constants.FAIL);
        operate.addElement("roomjid").addText(voteBean.getRoomJid());
        reply.setChildElement(queryResult);
		return reply;
	}
	
	public IQ getSuccessReplyIQ(IQ request,List<ConferenceVoteBean> voteList,ConferenceVoteBean voteBean){
		IQ reply = IQ.createResultIQ(request);
//		reply.setTo(userJid);
        Element queryResult = DocumentHelper.createElement(QName.get(Constants.PREFIX_NAME, Constants.SPACE_NAME));
        Element operate = queryResult.addElement("operate");
        operate.addAttribute("name", ActionFactory.LOAD_VOTE);
        operate.addElement("result").addText(Constants.SUCCESS);
        operate.addElement("roomjid").addText(voteBean.getRoomJid());
        
        Element votes = operate.addElement("votes");
        
        for(ConferenceVoteBean vb:voteList){
        	Element vote = votes.addElement("vote");
        	vote.addElement("id").addText(String.valueOf(vb.getId()));
        	vote.addElement("title").addText(vb.getTitle());
        	vote.addElement("title_remark").addText(vb.getTitleRemark());
        	vote.addElement("effective_time").addText(String.valueOf(vb.getEffectiveTime()));
        	vote.addElement("conference_id").addText(String.valueOf(vb.getConferenceId()));
        	vote.addElement("begin_time").addText(DateUtil.getDateStr(vb.getBeginTime(), "yyyy-MM-dd HH:mm:ss"));
        	vote.addElement("creator").addText(vb.getCreatorStr());
        	vote.addElement("is_start").addText(String.valueOf(vb.isStart()));
        	vote.addElement("is_close").addText(String.valueOf(vb.isClose()));
        	Element selects = vote.addElement("selects");
            for(ConferenceSelectBean selectBean:vb.getSelects()){
            	Element select = selects.addElement("select");
            	select.addElement("id").addText(String.valueOf(selectBean.getId())+"");
            	select.addElement("type").addText(String.valueOf(selectBean.getType())+"");
            	select.addElement("select_title").addText(selectBean.getSelectTitle()+"");
            	select.addElement("select_remark").addText(selectBean.getSelectRemark()+"");
            	select.addElement("select_num").addText(String.valueOf(selectBean.getSelectNum())+"");
            	Element options = select.addElement("options");
            	for(ConferenceOptionBean optionBean:selectBean.getOptions()){
            		Element option = options.addElement("option");
            		option.addElement("id").addText(String.valueOf(optionBean.getId())+"");
            		option.addElement("option_name").addText(optionBean.getOptionName()+"");
            	}
            }
        }
        reply.setChildElement(queryResult);
		return reply;
	}
	
	
	public ConferenceVoteBean parseXML(IQ request, Connection con,ConferenceVoteBean voteBean) throws Exception{
		PreparedStatement pstmt = null;
        boolean abortTransaction = false;
        ResultSet rs = null;
//		ConferenceVoteBean voteBean = new ConferenceVoteBean();
//		List<ConferenceSelectBean> selectBeans = new ArrayList<ConferenceSelectBean>();
//		Map<ConferenceSelectBean,List<ConferenceOptionBean>> optionMap = new HashMap<ConferenceSelectBean,List<ConferenceOptionBean>>();
		Element query = request.getChildElement();
		Element operate = query.element("operate");
        String command = operate.attributeValue("name");
        Element roomJid = operate.element("roomjid");
        String roomJidStr = roomJid.getText();
        voteBean.setRoomJid(roomJidStr);
        String confNo = roomJidStr.split("@")[0];
        if(confNo == null || confNo.trim().length()<1){
        	throw new Exception("会议ID为空");
        }else{
        	pstmt = con.prepareStatement("select id from conference where confno=? and del = 1");
	        pstmt.setString(1, confNo);
	        rs = pstmt.executeQuery();
	        if(rs.next()){
	        	int conferenceId = rs.getInt("id");
	        	voteBean.setConferenceId(conferenceId);
	        }else{
	        	throw new Exception("会议ID不存在");
	        }
	        DbConnectionManager.closeStatement(rs, pstmt);
        	
        }
        return voteBean;
	}

}
