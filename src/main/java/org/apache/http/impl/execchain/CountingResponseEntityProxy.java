package org.apache.http.impl.execchain;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.input.CountingInputStream;
import org.apache.http.HttpEntity;
import org.apache.http.conn.EofSensorInputStream;

public class CountingResponseEntityProxy extends ResponseEntityProxy {

	private CountingInputStream countingInputStream;
	
	public CountingResponseEntityProxy(HttpEntity entity, ConnectionHolder connHolder) {
		super(entity, connHolder);
	}

    public InputStream getContent() throws IOException {
        countingInputStream = new CountingInputStream(wrappedEntity.getContent());
		return new EofSensorInputStream(countingInputStream, this);
    }
    
    public CountingInputStream getCountingInputStream() {
		return countingInputStream;
	}
}
