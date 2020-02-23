package net.thisptr.phlegethon.blob.storage.s3;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import net.thisptr.phlegethon.blob.storage.BlobStorage;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

@Component
@Validated
@ConditionalOnProperty(name = "phlegethon.storage.type", havingValue = "s3")
@ConfigurationProperties(prefix = "phlegethon.storage.s3")
public class S3BlobStorageFactoryBean implements FactoryBean<BlobStorage> {
    public String endpoint;

    public String region;

    @NotEmpty
    public String bucketName;

    public String accessKeyId;

    public String secretAccessKey;

    @Override
    public Class<?> getObjectType() {
        return BlobStorage.class;
    }

    @Override
    public BlobStorage getObject() throws Exception {
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
                .enablePathStyleAccess();
        if (endpoint != null) {
            builder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region));
        } else if (region != null) {
            builder.withRegion(region);
        }
        if (accessKeyId != null && secretAccessKey != null)
            builder.withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKeyId, secretAccessKey)));
        return new S3BlobStorage(builder.build(), bucketName);
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getSecretAccessKey() {
        return secretAccessKey;
    }

    public void setSecretAccessKey(String secretAccessKey) {
        this.secretAccessKey = secretAccessKey;
    }
}
