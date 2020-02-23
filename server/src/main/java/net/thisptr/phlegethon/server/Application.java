package net.thisptr.phlegethon.server;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.thisptr.phlegethon.blob.storage.BlobStorage;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication(scanBasePackages = "net.thisptr.phlegethon")
public class Application {

    @Autowired
    public BlobStorage blobStorage;

    @Component
    @Validated
    @ConfigurationProperties(prefix = "phlegethon.db")
    public static class HikariDataSourceConfig implements FactoryBean<DataSource> {
        public String url;
        public String username;
        public String password;
        public Map<String, String> properties = new HashMap<>();

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public Map<String, String> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, String> properties) {
            this.properties = properties;
        }

        @Override
        public Class<?> getObjectType() {
            return DataSource.class;
        }

        @Override
        public DataSource getObject() throws Exception {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(username);
            config.setPassword(password);
            properties.forEach((k, v) -> {
                config.addDataSourceProperty(k, v);
            });
            return new HikariDataSource(config);
        }
    }
}
