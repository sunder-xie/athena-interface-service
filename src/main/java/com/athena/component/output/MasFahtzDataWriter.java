package com.athena.component.output;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;

import com.athena.component.exchange.FileLog;
import com.athena.component.exchange.config.DataParserConfig;
import com.athena.component.exchange.config.ExchangerConfig;
import com.athena.component.exchange.db.DBOutputTxtSerivce;
import com.athena.util.date.DateUtil;
import com.athena.util.exception.ServiceException;
import com.toft.utils.UUIDHexGenerator;

/**
 * 2840 、2850 保税库  发货通知 、发货通知明细
 * @author 贺志国
 * @E-mail zghe@isoftstone.com
 * @version v1.0
 * @date 2013-12-31
 */
public class MasFahtzDataWriter extends DBOutputTxtSerivce {

	public MasFahtzDataWriter(DataParserConfig dataParserConfig){
		
	}
	
	/**
	 * 生成文件头 
	 *  文件头格式固定：
	 *  	DEB  HERMES  PSA       FE1172220110124003525
	 * @throws IOException 
	 */
	@Override
	public void fileBefore(OutputStreamWriter writer) {
			try {
				writer.write("("+DateUtil.dateToStringYMDHms(new Date())+" ATHENA-MAS INTERFACE" + interfaceId + " BEGIN==>)");
				writer.write("\n");
			} catch (IOException e) {
	            logger.error("接口" + interfaceId + "IO输出异常", e);
	            throw new ServiceException("接口" + interfaceId + "IO输出异常", e);
			}
	}
	
	/**
	 * 记录全部输出完成后操作
	 * 生成文件结尾部分
	 */
	@Override
	public void afterAllRecords(ExchangerConfig[] ecs) {
        for (ExchangerConfig ec : ecs) {
            String encoding = ec.getEncoding();
            //默认为GBK
            if(encoding==null){
                encoding = "GBK";
            }
            File file = new File(ec.getFilePath() + File.separator + ec.getFileName());
            int num =  this.readFileLineNum(encoding,file);
			if(num>0){
				num --;
			}
			OutputStreamWriter out = writerFile(encoding,file);
			// 生成文件尾部
			try {
				out.write("("+DateUtil.dateToStringYMDHms(new Date())+ makefileEnd("ATHENA-MAS INTERFACE" + interfaceId + " END<==",num)+")");
				out.write("\n");
			} catch (IOException e) {
	            logger.error("接口" + interfaceId + "IO输出异常", e);
	            throw new ServiceException("接口" + interfaceId + "IO输出异常", e);
			}finally {
	            try {
	                out.close();
	            } catch (IOException e) {
	                logger.error("接口" + interfaceId + "无法关闭文件", e);
	                throw new ServiceException("接口" + interfaceId + "无法关闭文件", e);
	            }
	        }
        }
        super.afterAllRecords(ecs);
	}
	
	/**
	 * 生成文件尾部
	 * 
	 * @param str
	 * @return
	 */
	public String makefileEnd(String str,int num) {
		StringBuffer sb = new StringBuffer();
		sb.append(str);
		
		String total_Str = String.valueOf(num);
		
		//String total_Str1 = String.valueOf(dataEchange.total);
		if (total_Str != null) {
			if (total_Str.length() < 9) {
				// 生成9位长的字段
				int length = 9 - total_Str.length();
				for (int i = 0; i < length; i++) {
					sb.append("0");
				}
				sb.append(total_Str);
			} else {
				// 截取后九尾
				sb.append(total_Str.substring(0, 8));
			}
		} else {
			sb.append("000000000");
		}
		return sb.toString();
	}
	
	public OutputStreamWriter writerFile(String encoding,File file) {
		OutputStreamWriter writer = null;
		try {
			writer = new OutputStreamWriter(new FileOutputStream(file,true),encoding);
		} catch (FileNotFoundException e) {
			logger.error("接口" + dataParserConfig.getId() + "没有找到文件 " + e.getMessage());
			throw new ServiceException("接口" + interfaceId + "没有找到文件", e);
		} catch (UnsupportedEncodingException e) {
			logger.error("接口" + dataParserConfig.getId() + "不支持编码 " + e.getMessage());
			throw new ServiceException("接口" + interfaceId + "不支持编码", e);
		}
		 return writer;
	}
	
	
	public int readFileLineNum(String encoding,File file) {
		int result = 0;
        FileInputStream fileInputStreams = null;
		try {
			fileInputStreams =  new FileInputStream(file);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStreams,encoding));
			String line = null;
			while((line = bufferedReader.readLine()) != null){
				if(StringUtils.isNotEmpty(line.trim())){
					result ++;
				}
			}

		} catch (FileNotFoundException e) {
			logger.error("接口" + dataParserConfig.getId() + "没有找到文件 " + e.getMessage());
			throw new ServiceException("接口" + interfaceId + "没有找到文件", e);
		} catch (UnsupportedEncodingException e) {
			logger.error("接口" + dataParserConfig.getId() + "不支持编码 " + e.getMessage());
			throw new ServiceException("接口" + interfaceId + "不支持编码", e);
		} catch(IOException ex){
            logger.error("接口" + dataParserConfig.getId() + "IO输入出错 " + ex.getMessage());
            throw new ServiceException("接口" + interfaceId + "IO输入出错", ex);
        }finally {
            try {
            	fileInputStreams.close();
            } catch (IOException e) {
                logger.error("接口" + dataParserConfig.getId() + "无法关闭文件", e);
                throw new ServiceException("接口" + interfaceId + "无法关闭文件", e);
            }
        }
		return result;
	}
	
	/**
	 * 记录文件日志
	 * @author Hezg
	 * @date 2013-2-4
	 * @param wenjmc 文件名称
	 * @param file_satus 运行状态        1 成功(已执行)  -1 失败(已执行)
	 * @return 
	 */
	@Override
    protected void file_info(String wenjmc,boolean file_satus) {
        AtomicInteger insert_num = new AtomicInteger();
        AtomicInteger update_num = new AtomicInteger();
        AtomicInteger error_num = new AtomicInteger();
		Map<String,String> params = new HashMap<String,String>();
		params.put("SID", UUIDHexGenerator.getInstance().generate());
		params.put("INBH", interfaceId);
		params.put("fileName", wenjmc);
		params.put("file_begintime", file_begintime);
		params.put("file_endtime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
		params.put("insert_num", String .valueOf(insert_num));
		params.put("update_num", String .valueOf(update_num));
		params.put("error_num", String .valueOf(error_num));
		params.put("file_satus", file_satus?"1":"-1");
		
		FileLog.getInstance(sourceId).insert_file_info(params,dataParserConfig.getBaseDao());
	}
}
