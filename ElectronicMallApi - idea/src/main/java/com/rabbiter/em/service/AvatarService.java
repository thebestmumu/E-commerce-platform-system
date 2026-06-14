package com.rabbiter.em.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.rabbiter.em.constants.Constants;
import com.rabbiter.em.entity.Avatar;
import com.rabbiter.em.exception.ServiceException;
import com.rabbiter.em.mapper.AvatarMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class AvatarService {
    @Resource
    private AvatarMapper avatarMapper;

    public String upload(MultipartFile uploadFile){
        String url = null;
        //通过md5判断文件是否已经存在，防止在服务器存储相同文件
        InputStream inputStream = null;
        try {
            inputStream = uploadFile.getInputStream();
        } catch (IOException e) {
            log.error("读取头像文件输入流失败", e);
        }
        String md5 = SecureUtil.md5(inputStream);
        Avatar dbAvatar = avatarMapper.selectByMd5(md5);
        if(dbAvatar==null){
            String originalFilename = uploadFile.getOriginalFilename(); //文件原始名字
            String type = originalFilename.substring(originalFilename.lastIndexOf(".")+1);  //文件后缀
            log.debug("头像文件：{}, 类型：{}", originalFilename, type);
            long size = uploadFile.getSize() / 1024; //文件大小，单位kb
            //文件不存在，则保存文件
            File folder = new File(Constants.avatarFolderPath);
            if(!folder.exists()){
                folder.mkdir();
            }
            String folderPath = folder.getAbsolutePath()+"/";   //文件存储文件夹的位置
            log.debug("头像文件存储地址：{}", folderPath);


            //将文件保存为UUID的名字，通过uuid生成url
            String uuid = UUID.randomUUID().toString().replace("-", "").toLowerCase();
            String finalFileName = uuid+"."+type;
            File targetFile = new File(folderPath + finalFileName);
            try {
                uploadFile.transferTo(targetFile);
            } catch (IOException e) {
                log.error("头像文件保存失败：{}", finalFileName, e);
            }
            url = "/avatar/"+finalFileName;
            Avatar avatar = new Avatar(type, size, url, md5);
            log.debug("保存头像记录：{}", avatar);
            avatarMapper.save(avatar);
            return url;
        }
        return dbAvatar.getUrl();
    }
    //根据文件名下载文件
    public void download(String fileName, HttpServletResponse response){
        File file = new File(Constants.avatarFolderPath+fileName);
        if(!file.exists()){
            throw new ServiceException(Constants.CODE_500,"文件不存在");
        }
        try {
            ServletOutputStream os = response.getOutputStream();
            response.addHeader("Content-Disposition","attachment;filename="+ URLEncoder.encode(fileName,"UTF-8"));
            response.setContentType("application/octet-stream");
            os.write(FileUtil.readBytes(file));
            os.flush();
            os.close();
        } catch (IOException e) {
            log.error("头像文件下载失败：{}", fileName, e);
        }
    }

    public int delete(int id) {
        Avatar avatar = avatarMapper.selectById(id);
        int delete = avatarMapper.delete(id);
        log.debug("删除头像记录，影响行数：{}", delete);
        if(delete==1){
            String fileName = StrUtil.subAfter(avatar.getUrl(),"/",true);
            log.debug("待删除头像文件名：{}", fileName);
            File file = new File(Constants.avatarFolderPath+fileName);
            log.debug("待删除文件路径：{}", file.getAbsolutePath());
            if(file.exists()){

                boolean delete1 = file.delete();
                if(delete1){
                    log.debug("头像文件删除成功");
                }
            }
        }
        return delete;
    }

    public List<Avatar> selectPage(int index, int pageSize) {
        return avatarMapper.selectPage(index,pageSize);
    }

    public int getTotal() {
        return avatarMapper.getTotal();
    }
}
