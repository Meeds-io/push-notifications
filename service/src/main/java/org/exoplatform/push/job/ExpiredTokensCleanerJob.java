package org.exoplatform.push.job;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.push.service.DeviceService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class ExpiredTokensCleanerJob implements Job {

  private static final Log LOG = ExoLogger.getLogger(ExpiredTokensCleanerJob.class);

  private DeviceService deviceService;

  public ExpiredTokensCleanerJob() {
    this.deviceService = CommonsUtils.getService(DeviceService.class);
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    LOG.debug("Executing ExpiredTokensCleanerJob");
    deviceService.deleteDevicesWithExpiredToken();
  }
}
