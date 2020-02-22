package net.thisptr.phlegethon.server;

import net.thisptr.phlegethon.storage.BlobStorage;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "net.thisptr.phlegethon")
public class Application {

    @Autowired
    public BlobStorage blobStorage;
}
