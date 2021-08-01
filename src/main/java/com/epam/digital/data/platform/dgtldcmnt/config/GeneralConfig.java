package com.epam.digital.data.platform.dgtldcmnt.config;

import com.epam.digital.data.platform.bpms.client.config.FeignConfig;
import com.epam.digital.data.platform.integration.ceph.config.CephConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Import({CephConfig.class, FeignConfig.class})
public class GeneralConfig implements WebMvcConfigurer {

}
