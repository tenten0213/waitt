package net.unit8.waitt.feature.coverage;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.sourceforge.cobertura.coveragedata.CoverageDataFileHandler;
import java.util.logging.Logger;
import net.unit8.waitt.api.ServerMonitor;
import net.unit8.waitt.api.ClassLoaderFactory;
import net.unit8.waitt.api.ConfigurableFeature;
import net.unit8.waitt.api.EmbeddedServer;
import net.unit8.waitt.api.configuration.WebappConfiguration;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

/**
 * @author kawasima
 */
public class CoverageMonitor implements ServerMonitor,ConfigurableFeature {
    private static final Logger LOG = Logger.getLogger(CoverageMonitor.class.getName());
    private final CoverageMonitorConfiguration config;
    ExecutorService executorService;

    public CoverageMonitor() {
        Logger logger = Logger.getLogger(CoverageDataFileHandler.class.getName());
        logger.setUseParentHandlers(false);
        config = new CoverageMonitorConfiguration();
    }

    @Override
    public void config(WebappConfiguration webappConfig) {
        config.setSourceDirectory(webappConfig.getSourceDirectory());
        config.setTargetPackages(webappConfig.getPackages());
    }
    
    @Override
    public void init(EmbeddedServer server) {
        final ClassRealm coverageRealm = (ClassRealm) this.getClass().getClassLoader();
        server.setClassLoaderFactory(new ClassLoaderFactory() {
            @Override
            public ClassLoader create(ClassLoader parent) {
                coverageRealm.setParentClassLoader(parent);
                CoberturaClassLoader ccl = CoberturaClassLoader.create(coverageRealm);
                ccl.setTargetPackages(config.getTargetPackages());
                return ccl;
            }
        });
    }
    
    @Override
    public void start(EmbeddedServer server) {
        File reportDirectory = new File("target/coverage");
        if (!reportDirectory.exists()) {
            reportDirectory.mkdirs();
        }
        config.setCoverageReportDirectory(reportDirectory);
        server.addContext("/_coverage", reportDirectory.getAbsolutePath(), getClass().getClassLoader());
        executorService = Executors.newCachedThreadPool();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        CoberturaClassLoader loader = CoberturaClassLoader.getInstance();
                        LOG.info("Start reporting Coverage report");
                        executorService.execute(new ReportGenerator(loader, config));
                        break;
                    } catch (IllegalStateException e) {
                        try {
                            TimeUnit.SECONDS.sleep(5);
                        } catch (InterruptedException ex) {
                            break;
                        }
                    }
                }
            }
        });
    }
    
    @Override
    public void stop() {
        executorService.shutdown();
    }
}
