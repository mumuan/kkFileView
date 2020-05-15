package cn.keking.service.impl;

import cn.keking.config.ConfigConstants;
import cn.keking.model.FileAttribute;
import cn.keking.model.ReturnResponse;
import cn.keking.service.FilePreview;
import cn.keking.utils.CadToPdf;
import cn.keking.utils.DownloadUtils;
import cn.keking.utils.FileUtils;
import cn.keking.utils.PdfUtils;
import cn.keking.web.filter.BaseUrlFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;

import static cn.keking.service.impl.OfficeFilePreviewImpl.getPreviewType;

/**
 * @author chenjh
 * @since 2019/11/21 14:28
 */
@Service
public class CadFilePreviewImpl implements FilePreview {

    private String fileDir = ConfigConstants.getFileDir();

    @Autowired
    private FileUtils fileUtils;

    @Autowired
    private DownloadUtils downloadUtils;

    @Autowired
    private CadToPdf cadToPdf;

    @Autowired
    private PdfUtils pdfUtils;

    public static final String OFFICE_PREVIEW_TYPE_PDF = "pdf";
    public static final String OFFICE_PREVIEW_TYPE_IMAGE = "image";
    public static final String OFFICE_PREVIEW_TYPE_ALLIMAGES = "allImages";

    @Override
    public String filePreviewHandle(String url, Model model, FileAttribute fileAttribute) {
        // 预览Type，参数传了就取参数的，没传取系统默认
        String officePreviewType = model.asMap().get("officePreviewType") == null ? ConfigConstants.getOfficePreviewType() : model.asMap().get("officePreviewType").toString();
        String baseUrl = BaseUrlFilter.getBaseUrl();
        String suffix=fileAttribute.getSuffix();
        String fileName=fileAttribute.getName();
        String pdfName = fileName.substring(0, fileName.lastIndexOf(".") + 1) + "pdf";
        String outFilePath = fileDir + pdfName;
        // 判断之前是否已转换过，如果转换过，直接返回，否则执行转换
        if (!fileUtils.listConvertedFiles().containsKey(pdfName) || !ConfigConstants.isCacheEnabled()) {
            String filePath;
            ReturnResponse<String> response = downloadUtils.downLoad(fileAttribute, null);
            if (0 != response.getCode()) {
                model.addAttribute("fileType", suffix);
                model.addAttribute("msg", response.getMsg());
                return "fileNotSupported";
            }
            filePath = response.getContent();
            if (StringUtils.hasText(outFilePath)) {
                boolean convertResult = cadToPdf.cadToPdf(filePath, outFilePath);
                if (!convertResult) {
                    model.addAttribute("fileType", suffix);
                    model.addAttribute("msg", "cad文件转换异常，请联系管理员");
                    return "fileNotSupported";
                }
                if (ConfigConstants.isCacheEnabled()) {
                    // 加入缓存
                    fileUtils.addConvertedFile(pdfName, fileUtils.getRelativePath(outFilePath));
                }
            }
        }
        if (baseUrl != null && (OFFICE_PREVIEW_TYPE_IMAGE.equals(officePreviewType) || OFFICE_PREVIEW_TYPE_ALLIMAGES.equals(officePreviewType))) {
            return getPreviewType(model, fileAttribute, officePreviewType, baseUrl, pdfName, outFilePath, pdfUtils, OFFICE_PREVIEW_TYPE_IMAGE);
        }
        model.addAttribute("pdfUrl", pdfName);
        return "pdf";
    }


}
