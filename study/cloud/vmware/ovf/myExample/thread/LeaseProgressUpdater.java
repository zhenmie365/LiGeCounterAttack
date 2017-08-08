package com.sunment.cloud.vmware.client.res.ovf.thread;

import com.vmware.vim25.mo.HttpNfcLease;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LeaseProgressUpdater extends Thread
{
  private Logger logger = LoggerFactory.getLogger(LeaseProgressUpdater.class);
  private HttpNfcLease httpNfcLease = null;
  private int progressPercent = 0;
  private int updateInterval;

  public LeaseProgressUpdater(HttpNfcLease httpNfcLease, int updateInterval) 
  {
    this.httpNfcLease = httpNfcLease;
    this.updateInterval = updateInterval;
  }

  public void run() 
  {
    logger.info("httpNfcLease Thread.run() is begin!");
    while (true) 
    {
      try
      {
        httpNfcLease.httpNfcLeaseProgress(progressPercent);
        Thread.sleep(updateInterval);
      }
      catch(InterruptedException ie)
      {
        break;
      }
      catch(Exception e)
      {
        throw new RuntimeException(e);
      }
    }

    logger.info("httpNfcLease Thread.run() is over!");
  }
  
  public void setPercent(int percent)
  {
    this.progressPercent = percent;
  }
}