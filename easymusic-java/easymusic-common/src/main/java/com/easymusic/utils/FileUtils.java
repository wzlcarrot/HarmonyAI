package com.easymusic.utils;

import com.easymusic.entity.config.AppConfig;
import com.easymusic.entity.constants.Constants;
import com.easymusic.entity.enums.DateTimePatternEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Date;

@Component
@Slf4j
public class FileUtils {

    @Resource
    private AppConfig appConfig;

    public String uploadFile(MultipartFile file){
       String folderName = DateUtil.format(new Date(), DateTimePatternEnum.YYYY_MM_DD.getPattern())+"/";

       String folderPath = appConfig.getProjectFolder()+ Constants.FILE_FOLDER + folderName;
        File folder = new File(folderPath);
        if(folder.exists()==false){
            folder.mkdirs();
        }
        String fileName = System.currentTimeMillis()+StringTools.getFileSuffix(file.getOriginalFilename());

        try {
            file.transferTo(new File(folderPath+fileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return folderName+fileName;
    }


    public String downloadFile(String url, String suffix) {
        String folderName = DateUtil.format(new Date(), DateTimePatternEnum.YYYYMMDD.getPattern());
        String avatarFolderPath = appConfig.getProjectFolder() + Constants.FILE_FOLDER + folderName + "/";
        File folder = new File(avatarFolderPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        String fileName = StringTools.getRandomNumber(Constants.LENGTH_30) + suffix;
        String filePath = avatarFolderPath + fileName;
        log.info("filePath:"+filePath);
        OKHttpUtils.download(url, filePath);
        return folderName + "/" + fileName;
    }
}
