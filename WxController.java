package com.bjsxt.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bjsxt.service.ReqEventMessageService;
import com.bjsxt.service.ReqOtherMessageService;
import com.bjsxt.service.ReqTextMessageService;
import com.bjsxt.utils.XmlUtils;
import com.bjsxt.vo.MsgType;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping(value = "/wx")
public class WxController extends HttpServlet{
	private static Logger logger = Logger.getLogger(WxController.class);

	@Autowired
    ReqTextMessageService reqTextMessageService;

	@Autowired
    ReqEventMessageService reqEventMessageService;

	@Autowired
    ReqOtherMessageService reqOtherMessageService;

	@Override
	@RequestMapping(value ="/wx.action",method = RequestMethod.GET)
	protected void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
		//设置字符集
		req.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");
		//获取输出
		PrintWriter out = response.getWriter();
		//接入 调用方法 处理接入
		connect(req,out);
	}

	@Override
	@RequestMapping(value ="/wx.action",method = RequestMethod.POST)
	protected void doPost(HttpServletRequest req, HttpServletResponse response) {
		try {
			req.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			PrintWriter out = response.getWriter();
			responseMessage(req,out);
		} catch (IOException e) {
			logger.info(e);
		}
	}


	private void connect(HttpServletRequest req, PrintWriter out) {
		//获取参数
		String signature = req.getParameter("signature");
		String timestamp = req.getParameter("timestamp");
		String nonce = req.getParameter("nonce");
		String echostr = req.getParameter("echostr");
		//校验
		List<String> list = new ArrayList<String>();
		list.add("fxw");
		list.add(timestamp);
		list.add(nonce);
		//排序
		Collections.sort(list);
		//拼接字符串--》sha1加密
		StringBuilder sb = new StringBuilder();
		for(String e:list){
			sb.append(e);
		}
		//加密
		String sha1Str = DigestUtils.sha1Hex(sb.toString());
		//对比
		boolean flag = sha1Str.equals(signature);
		if(flag){
			System.out.println("恭喜接入成功!");
			out.print(echostr);
			out.flush();
		}
	}
	
	private void responseMessage(HttpServletRequest req, PrintWriter out){
		try {
			ServletInputStream inputStream = req.getInputStream();
			Map<String, String> requestMap = XmlUtils.streamToMap(inputStream);
			String msgType = requestMap.get("MsgType");
			String response = null;
			//接收的是文本消息
			if(msgType.equals(MsgType.TEXT.getCode())){
                String send = reqTextMessageService.send(requestMap);
                response = send;
            }else if(msgType.equals(MsgType.EVENT.getCode())){
                String send = reqEventMessageService.send(requestMap);
                response = send;
			}else{
                String send = reqOtherMessageService.send(requestMap);
                response = send;
			}
			logger.info("resp:" + response);
			out.print(response);
			out.flush();
		} catch (Exception e) {
			logger.info(e);
		}
	}

}
