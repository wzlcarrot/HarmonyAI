package com.easymusic.controller;


import com.easymusic.entity.config.AppConfig;
import com.easymusic.entity.constants.Constants;
import com.easymusic.entity.enums.ResponseCodeEnum;
import com.easymusic.exception.BusinessException;
import com.easymusic.utils.StringTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@RestController
@Slf4j
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController extends ABaseController{

    private final AppConfig appConfig;

    // 支持的音频格式
    private static final Set<String> AUDIO_TYPES = new HashSet<>(Arrays.asList(
            ".mp3", ".wav", ".ogg", ".aac", ".flac", ".m4a"
    ));

    // 支持的图片格式
    private static final Set<String> IMAGE_TYPES = new HashSet<>(Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"
    ));

    //http://localhost:8090/api/file/getResource?filePath=202508/0L1aeiSXp1c31CdlD280A1MnnhnIde.png
    //http://localhost:3006/api/file/getResource?filePath=202508/0L1aeiSXp1c31CdlD280A1MnnhnIde.png
    @RequestMapping("/getResource")
    public void getResource(HttpServletResponse response,
                            @RequestHeader(value = "Range", required = false) String rangeHeader,
                            @NotNull String filePath) {
        if (StringTools.pathIsOk(filePath) == false) {
            log.info("1234");
            throw new BusinessException(ResponseCodeEnum.CODE_600);

        }
        log.info("12345678");
        String suffix = StringTools.getFileSuffix(filePath);

        log.info("suffix:"+suffix);
        //设置响应头，采用MIME类型
        if (suffix != null) {
            if (IMAGE_TYPES.contains(suffix.toLowerCase())) {
                response.setContentType("image/" + suffix.replace(".", ""));
            } else if (AUDIO_TYPES.contains(suffix.toLowerCase())) {
                response.setContentType("audio/" + suffix.replace(".", ""));
            } else {
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
        } else {
            // 如果没有文件后缀，默认设置为jpeg格式
            response.setContentType("image/jpeg");
        }
        response.setHeader("Cache-Control", "max-age=2592000");
        readFile(response, rangeHeader, filePath);

    }

    //读文件
    protected void readFile(HttpServletResponse response, String rangeHeader, String filePath) {
        File file = new File(appConfig.getProjectFolder() + Constants.FILE_FOLDER + filePath);
        log.info("filePath:"+(appConfig.getProjectFolder() + Constants.FILE_FOLDER + filePath));
        //检查文件是否存在
        if (!file.exists()) {
            return;
        }

        try {
            long fileLength = file.length();
            long start = 0;
            long end = fileLength - 1;

            // 解析Range请求头
            if (rangeHeader != null && !rangeHeader.isEmpty()) {
                // Range: bytes=start-end 格式
                String[] rangeParts = rangeHeader.replace("bytes=", "").split("-");
                if (rangeParts.length >= 1 && !rangeParts[0].isEmpty()) {
                    start = Long.parseLong(rangeParts[0]);
                }
                if (rangeParts.length >= 2 && !rangeParts[1].isEmpty()) {
                    end = Long.parseLong(rangeParts[1]);
                }

                // 确保范围不超过文件大小
                if (end >= fileLength) {
                    end = fileLength - 1;
                }

                // 设置状态码为206 Partial Content
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

                // 设置Content-Range响应头
                response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
            } else {
                // 普通请求，设置状态码为200 OK
                response.setStatus(HttpServletResponse.SC_OK);
            }

            // 设置Content-Length响应头
            long contentLength = end - start + 1;
            response.setHeader("Content-Length", String.valueOf(contentLength));

            // 使用RandomAccessFile进行范围读取
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
                 OutputStream out = response.getOutputStream()) {

                // 定位到起始位置
                randomAccessFile.seek(start);

                // 读取并发送指定范围的数据
                byte[] buffer = new byte[8192]; // 8KB缓冲区
                long remaining = contentLength;
                int read;

                while (remaining > 0 && (read = randomAccessFile.read(buffer, 0,
                        (int) Math.min(buffer.length, remaining))) != -1) {
                    out.write(buffer, 0, read);
                    remaining -= read;
                }
                out.flush();
            }
        } catch (Exception e) {
            log.error("读取文件异常", e);
        }
    }

}
