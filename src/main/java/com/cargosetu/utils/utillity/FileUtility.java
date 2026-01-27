package com.cargosetu.utils.utillity;



import com.cargosetu.exception.FileNotCreatedException;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


@Slf4j
@Component
public class FileUtility {

    private static final String MODULE = " [File Utility ] ";



    public String saveFileToServer(MultipartFile argFile, String path) throws IOException {
        String SUBMODULE = MODULE + " [saveFileToServer()] ";
        Assert.notNull(path, "Path should not be empty");
        String fileName = "Test";

        int allowedFileSize = 2;
        if (argFile.getSize() > ((long) allowedFileSize * 1024 * 1024))
            throw new RuntimeException("File size limit exceeds. Please provide document within " + allowedFileSize + "MB");

        if (null != argFile) {
            fileName = (null != argFile.getOriginalFilename()) ? argFile.getOriginalFilename().replace("/", "_").trim() : fileName;
        }
//        path="D:\\";
        File file = new File(path + System.currentTimeMillis() + "_" + fileName);
        File directory = new File(path);
        try {
            if (!directory.exists()) {
                directory.mkdir();
            }
            boolean isCreated = file.createNewFile();
            if (!isCreated) {
                throw new FileNotCreatedException();
            }
            if (null != argFile) {
                FileOutputStream fout = new FileOutputStream(file);
                fout.write(argFile.getBytes());
                fout.close();
            }
            return file.getName();
        } catch (IOException e) {
            log.error(SUBMODULE + e.getMessage(), e);
            e.printStackTrace();
        }
        return file.getName();
    }

    /** Generic method for List Of Multipart Files */
    public HashMap<String,StringBuilder> saveMultipleFiles(MultipartFile[] files, String path) throws IOException {
        StringBuilder uniqueNames = new StringBuilder();
        StringBuilder fileNames = new StringBuilder();
        HashMap<String,StringBuilder> map = new HashMap<>();
        log.info(":::::::::::::::Inside Save Multiple Files Method::::::::::::::::::");
        if (files != null) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String uniqueName = saveFile(file, path);
                    if (fileNames.length() > 0) {
                        fileNames.append(",");
                    }
                    fileNames.append(file.getOriginalFilename());
                    if (uniqueNames.length() > 0) {
                        uniqueNames.append(",");
                    }
                    uniqueNames.append(uniqueName);
                    map.put("uniqueNames",uniqueNames);
                    map.put("fileNames",fileNames);
                }
            }
        }
        return map;
    }

    public String saveFile(MultipartFile argFile, String path) throws IOException {
        Assert.notNull(path, "Path should not be empty");
        String fileName = "Test";
        Path fullPath = Paths.get(path);
        File directory = fullPath.toFile();
        if (!directory.exists()) {
            boolean dirsCreated = directory.mkdirs();
            if (!dirsCreated) {
                throw new IOException("Failed to create directories: " + fullPath);
            }
        }
        if (null != argFile) {
            fileName = (null != argFile.getOriginalFilename()) ? argFile.getOriginalFilename().replace("/", "_").trim() : fileName;
        }
        File file = new File(path+ System.currentTimeMillis() + "_" + fileName);
        try (FileOutputStream fout = new FileOutputStream(file)) {
            // Write file contents
            fout.write(argFile.getBytes());
            log.info("File Uploaded successfully on path: "+path);
        } catch (IOException e) {
            log.error(":::::Exception While Saving File:::: " + e.getMessage(), e);
            throw new FileNotCreatedException();
        }
        return file.getName();
    }


    

    public boolean removeFileAtServer(String argFile, String path) {
        boolean isFileDeleted = false;
        String SUBMODULE = MODULE + " [saveFileToServer()] ";
        Assert.notNull(path, "Path should not be empty");
        try {
            File file = new File(path + argFile);
            if (null == file) {
                log.debug(SUBMODULE + "File not found with name" + file.getName());
            }
            if (null != file && file.delete()) {
                isFileDeleted = true;
            }
            return isFileDeleted;
        } catch (Exception ex) {
            log.error(SUBMODULE + ex.getMessage(), ex);
            throw ex;
        }
    }

    public boolean removeFileAtServer(String path) {
        boolean isFileDeleted = false;
        String SUBMODULE = MODULE + " [saveFileToServer()] ";
        Assert.notNull(path, "Path should not be empty");
        try {
            File file = new File(path);
            if (null == file) {
                log.debug(SUBMODULE + "File not found with name" + file.getName());
            }
            if (null != file && file.delete()) {
                isFileDeleted = true;
            }
            return isFileDeleted;
        } catch (Exception ex) {
            log.error(SUBMODULE + ex.getMessage(), ex);
            throw ex;
        }
    }

    public MultipartFile getFileFromArray(String fileName, MultipartFile[] files) {
        boolean isAvailable = false;
        try {
            Integer allowedFileSize = 2;
            for (MultipartFile file : files) {
                if (file.getSize() > (allowedFileSize * 1024 * 1024))
                    throw new RuntimeException("File size limit exceeds. Please provide document within " + allowedFileSize + "MB");
                if (fileName.equalsIgnoreCase(file.getOriginalFilename())) {
                    isAvailable = true;
                    return file;
                }
            }
        } catch (Exception ex) {
            throw ex;
        }
        return null;
    }



    public <T> List<T> readCsv(Class<T> clazz, InputStream stream) throws IOException {
        try {
            CsvMapper mapper = new CsvMapper();
            CsvSchema schema = mapper.typedSchemaFor(clazz)
                    .withColumnSeparator(CsvSchema.DEFAULT_COLUMN_SEPARATOR)
                    .withHeader()
                    .withColumnReordering(true)
                    .withArrayElementSeparator(CsvSchema.DEFAULT_ARRAY_ELEMENT_SEPARATOR)
                    .withNullValue(StringUtils.EMPTY)
                    .withoutEscapeChar();
            return mapper
                    .readerFor(clazz)
                    .with(CsvParser.Feature.TRIM_SPACES)
                    .with(CsvParser.Feature.SKIP_EMPTY_LINES)
                    .with(schema)
                    .<T>readValues(stream)
                    .readAll();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Exception While reading CSV file: "+ex.getMessage());
        }
    }



    public File convertMultipartFileToFile(MultipartFile multipartFile) throws IOException {
        File file = new File(multipartFile.getOriginalFilename());
        try (OutputStream outputStream = new FileOutputStream(file)) {
            InputStream inputStream = multipartFile.getInputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        return file;
    }

    /**
     * Generic Method for Delete Directory.
     * @author Kalp Shah
     * @param folderToDelete
     *
     * */
    public static void deleteDirectory(File folderToDelete) {
        if (folderToDelete.exists() && folderToDelete.isDirectory()) {
            try {
                org.apache.commons.io.FileUtils.deleteDirectory(folderToDelete);
                System.out.println("Deleted folder: " + folderToDelete.getAbsolutePath());
            } catch (Exception e) {
                System.err.println("Error deleting folder: " + e.getMessage());
            }
        } else {
            System.out.println("Folder does not exist: " + folderToDelete.getAbsolutePath());
        }
    }

}
