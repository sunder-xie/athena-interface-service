package com.athena.component.input;

import java.util.Date;

import com.athena.component.exchange.Record;
import com.athena.component.exchange.config.DataParserConfig;
import com.athena.component.exchange.txt.TxtInputDBSerivce;
import com.athena.util.exception.ServiceException;

/**
 *  2810 零件供应商临时包装
 * @author hzg
 *
 */
public class LjGysLsBzDBDataWriter extends TxtInputDBSerivce {

	private  Date date = new Date();
	public LjGysLsBzDBDataWriter(DataParserConfig dataParserConfig) {
		super(dataParserConfig);
	}
	
	/**
	 * 解析数据之前清空ckx_lingjgyslsb 中间表数据
	 */
	@Override
	public void before() {
		try{
			dataParserConfig.getBaseDao().getSdcDataSource(dataParserConfig.getWriterConfig().getDatasourceId())
			.execute("inPutzxc.lingjgyslsbzDelete");
		}catch(RuntimeException e)
		{
			logger.error("线程--接口" + interfaceId +"清除ckx_lingjgyslsbz表时报错"+e.getMessage());
			throw new ServiceException("线程--接口" + interfaceId +"清除ckx_lingjgyslsbz表时报错"+e.getMessage());
		}
	}
	
	/**
	 * 行记录解析之后   给行记录增加创建人、创建时间、修改人、修改时间
	 * */
	@Override
	public boolean afterRecord(Record record){	
		record.put("CREATOR", interfaceId);
		record.put("CREATE_TIME", date);
		record.put("EDITOR", interfaceId);
		record.put("EDIT_TIME", date);
		return true;
	}

    @Override
    public void after() {
        //对主表标记editor为temp
        dataParserConfig.getBaseDao().getSdcDataSource(dataParserConfig.getWriterConfig().getDatasourceId())
                .execute("inPutzxc.lingjgyslsbzUpdateTemp");
        //对主表与中间表进行合并
        dataParserConfig.getBaseDao().getSdcDataSource(dataParserConfig.getWriterConfig().getDatasourceId())
                .execute("inPutzxc.lingjgyslsbzMerge");
        //删除主表标记editor为temp的数据
        dataParserConfig.getBaseDao().getSdcDataSource(dataParserConfig.getWriterConfig().getDatasourceId())
                .execute("inPutzxc.lingjgyslsbzDeleteTemp");
    }


}
