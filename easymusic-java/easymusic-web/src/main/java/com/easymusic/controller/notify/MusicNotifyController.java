package com.easymusic.controller.notify;


import com.easymusic.controller.ABaseController;
import com.easymusic.service.MusicInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
@RestController
@RequestMapping("/musicNotify")
@Slf4j
@RequiredArgsConstructor
public class MusicNotifyController extends ABaseController {

    private final MusicInfoService musicInfoService;
    @RequestMapping("/tianpuyu/{musicType}")
    public String tianpuyueMusicCreateNotify(@PathVariable("musicType") Integer musicType,
                                             @RequestBody String responseJson) {
        log.info("音乐创建回到信息musicType:{},body:{}", musicType, responseJson);
        musicInfoService.musicCreateNotify(musicType, responseJson);

        log.info("进行了回调过程");
        return STATUS_SUCCESS;
    }
}
