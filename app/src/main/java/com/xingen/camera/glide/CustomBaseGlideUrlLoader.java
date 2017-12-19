package com.xingen.camera.glide;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelCache;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.load.model.stream.BaseGlideUrlLoader;

import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ${xingen} on 2017/7/5.
 */

public class CustomBaseGlideUrlLoader extends BaseGlideUrlLoader<String> {
    private static final ModelCache<String, GlideUrl> urlCache =
            new ModelCache<>(150);
    /**
     * Url的匹配规则
     */
    private static final Pattern PATTERN = Pattern.compile("__w-((?:-?\\d+)+)__");

    public CustomBaseGlideUrlLoader(ModelLoader<GlideUrl, InputStream> concreteLoader, ModelCache<String, GlideUrl> modelCache) {
        super(concreteLoader,modelCache);
    }

    /**
     *
     * 控制加载的图片的大小
     */
    @Override
    protected String getUrl(String model, int width, int height, Options options) {
        Matcher m = PATTERN.matcher(model);
        int bestBucket = 0;
        if (m.find()) {
            String[] found = m.group(1).split("-");
            for (String bucketStr : found) {
                bestBucket = Integer.parseInt(bucketStr);
                if (bestBucket >= width) {
                    // the best bucket is the first immediately bigger than the requested width
                    break;
                }
            }
            if (bestBucket > 0) {
                model = m.replaceFirst("w"+bestBucket);
            }
        }
        return model;
    }

    @Override
    public boolean handles(String s) {
        return true;
    }

    /**
     * 工厂来构建CustormBaseGlideUrlLoader对象
     */
    public static class Factory implements ModelLoaderFactory<String,InputStream> {
        @Override
        public ModelLoader<String, InputStream> build(MultiModelLoaderFactory multiFactory) {
            return new CustomBaseGlideUrlLoader(multiFactory.build(GlideUrl.class,InputStream.class),urlCache);
        }
        @Override
        public void teardown() {

        }
    }
}
