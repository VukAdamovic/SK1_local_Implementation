package sk_projekat1;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import sk_projekat1.enums.TypeFilter;
import sk_projekat1.enums.TypeSort;
import sk_projekat1.exceptions.CustomException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class ImplementationLocal implements Storage{

    static {
        StorageManager.registerStorage(new ImplementationLocal());
    }

    //dovrsi metodu
    @Override
    public boolean setPath(String apsolutePath) {
        File storage = new File(apsolutePath);
        File configFile;
        boolean operation = false;


        if(!storage.exists()){
            return  false;
        }
        else{
            for(File file : Objects.requireNonNull(storage.listFiles())){
                if(file.getName().equals("CONFIGURATION.txt")){
                    try {
                        List<String> configAtributes = new ArrayList<>();
                        configFile = file;
                        Scanner myReader = new Scanner(configFile);

                        while(myReader.hasNextLine()){
                            String line = myReader.nextLine();
                            String[] value = line.split(":");
                            configAtributes.add(value[1]);
                        }

                        StorageArguments.name = configAtributes.get(0);
                        StorageArguments.path = apsolutePath;
                        StorageArguments.totalSpace = Integer.parseInt(configAtributes.get(1));
                        StorageArguments.restrictedExtensions = Collections.singletonList(configAtributes.get(2));
                        StorageArguments.maxFilesInStorage = Integer.parseInt(configAtributes.get(3));
                        //usedSpace trebam da dodam
                        StorageArguments.fileNumberInStorage = searchFilesInFolders("",TypeSort.ALPHABETICAL_ASC, TypeFilter.FILE_EXTENSION).size();
                        operation=true;
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return  operation;
    }

    @Override
    public boolean createStorage(String storageName, String storagePath, int storageSize, String storageRestrictedExtensions, int maxFilesInStorage) {
        File dir = new File(storagePath, storageName);

        StorageArguments.name = storageName;
        StorageArguments.path = dir.getAbsolutePath();

        StorageArguments.totalSpace = storageSize;
        StorageArguments.usedSpace = 0;

        StorageArguments.restrictedExtensions = new ArrayList<>();
        String[] resExe = storageRestrictedExtensions.split(",");
        StorageArguments.restrictedExtensions.addAll(Arrays.asList(resExe));

        StorageArguments.maxFilesInStorage = maxFilesInStorage;

        boolean mkdirs = dir.mkdirs();
        File configFile = new File(dir.getAbsolutePath(), "CONFIGURATION.txt");
        try {
            FileWriter fileWriter = new FileWriter(configFile);
            fileWriter.write("Storage name:" + storageName + "\n");
            fileWriter.write("Storage size in bytes:" + storageSize + "\n");
            fileWriter.write("Storage restricted extensions:" + storageRestrictedExtensions + "\n");
            fileWriter.write("Storage max file size number:" + maxFilesInStorage);
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return mkdirs;

    }

    @Override
    public boolean createDefaultStorage() {
        File dir = new File("D:/defaultsStorageResources", "DefaultStorage" + StorageArguments.counter);
        StorageArguments.name = "DefaultStorage" + StorageArguments.counter;
        StorageArguments.counter++;
        StorageArguments.path = dir.getAbsolutePath();
        StorageArguments.totalSpace = 250;
        StorageArguments.usedSpace = 0;
        StorageArguments.restrictedExtensions = new ArrayList<>();
        StorageArguments.maxFilesInStorage = 8;


        boolean mkdirs = dir.mkdirs();
        File configFile = new File(dir.getAbsolutePath(), "CONFIGURATION.txt");
        try {
            FileWriter fileWriter = new FileWriter(configFile);
            fileWriter.write("Storage name:" + StorageArguments.name + "\n");
            fileWriter.write("Storage size in bytes:" + StorageArguments.totalSpace + "\n");
            fileWriter.write("Storage restricted extensions:" + StorageArguments.restrictedExtensions + "\n");
            fileWriter.write("Storage max file size number:" + StorageArguments.maxFilesInStorage);
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return mkdirs;
    }

    @Override
    public boolean createFolder(String folderName, String folderPath) {
        if(folderPath.equals(".")){
            folderPath="";
        }
        File dir = new File(StorageArguments.path + folderPath, folderName);
        if (!dir.exists()) {
            boolean mkdirs = dir.mkdirs();
            return mkdirs;
        } else {
            throw  new CustomException("Action FAILED \t Folder: " + dir.getAbsolutePath() + " already exists");
        }
    }

    @Override
    public boolean createFile(String fileName, String filePath) {
        if(filePath.equals(".")){
            filePath="";
        }

        File file = new File(StorageArguments.path + filePath, fileName);

        if (!file.exists()) {
            try {

                if (StorageArguments.restrictedExtensions.contains(FilenameUtils.getExtension(fileName))) {
                    throw new CustomException("Action FAILED \t Storage unsupported extensions:" + StorageArguments.restrictedExtensions);
                }

                FileWriter fileWriter = new FileWriter(file);
                fileWriter.write("Ubacio sam tekst samo da bih mogao da testiram za prekoracenu velicinu u storagu");
                fileWriter.close();

                if (StorageArguments.fileNumberInStorage + 1 > StorageArguments.maxFilesInStorage) {
                    throw new CustomException("Action FAILED \t Storage limit max files:" + StorageArguments.maxFilesInStorage);
                }

                if (StorageArguments.usedSpace + file.length() > StorageArguments.totalSpace) {
                    throw new CustomException("Action FAILED \t Storage byte size:" + StorageArguments.totalSpace);
                }

                StorageArguments.fileNumberInStorage += 1;
                StorageArguments.usedSpace = (int) (StorageArguments.usedSpace + file.length());

                return true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new CustomException("Action FAILED \t File:" + file.getAbsolutePath() + " already exists.");
        }
    }

    @Override
    public boolean moveFile(String oldFilePath, String movePath) {
        File targetFile = new File(StorageArguments.path + oldFilePath); // objekat koji zelimo da premestimo
        File targetLocation = Path.of(StorageArguments.path + movePath).toFile(); // ciljana lokacija

        if (targetFile.exists() && targetLocation.exists() && targetLocation.isDirectory()) {
            try {
                File folderLoc = new File(targetLocation.getAbsolutePath() + "/" + targetFile.getName());
                Files.move(Path.of(targetFile.getPath()), Path.of(folderLoc.getPath()));
                return true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (!targetFile.exists()) {
            throw new CustomException("Action FAILED \t File: " + targetFile.getAbsolutePath() + " does not exists");
        } else if (!targetLocation.exists()) {
            throw new CustomException("Action FAILED \t Target path: " + targetLocation.getAbsolutePath() + " does not exists");
        } else if (!targetLocation.isDirectory()) {
            throw new CustomException("Action FAILED \t Target location" + targetLocation.getAbsolutePath() + " is not a folder");
        }
        return false;
    }

    @Override
    public boolean renameFileObject(String foNewName, String foPath) {
        File fo = new File(StorageArguments.path + foPath);
        if (fo.exists()) {
            File renamedFile = new File(fo.getParentFile().getAbsolutePath(), foNewName);
            return fo.renameTo(renamedFile);
        } else {
            throw new CustomException("Action FAILED \t " + fo.getAbsolutePath() + " does not exists");
        }
    }

    @Override
    public boolean deleteFileObject(String folderPath) {
        File fo = new File(StorageArguments.path + folderPath);
        if (fo.exists()) {
            return fo.delete();
        } else {
            throw new CustomException("Action FAILED \t " + fo.getAbsolutePath() + " does not exists");
        }
    }

    @Override
    public boolean importFileObject(String[] importLocalPaths, String importStoragePath) {
        File storageFile = new File(StorageArguments.path + importStoragePath);

        if (!storageFile.exists()) {
            throw new CustomException("Action FAILED \t File: " + storageFile.getAbsolutePath() + " does not exists");
        }
        if (!storageFile.isDirectory()) {
            throw new CustomException("Action FAILED \t"+storageFile.getAbsolutePath() + "is not a folder");
        }

        for (String importLocalPath : importLocalPaths) {
            File localFile = new File(importLocalPath);

            if (localFile.exists()) {
                try {
                    if (localFile.isDirectory()) {
                        FileUtils.copyDirectoryToDirectory(localFile, storageFile);

                    } else if (localFile.isFile()) {

                        if (StorageArguments.fileNumberInStorage + 1 > StorageArguments.maxFilesInStorage) {
                            throw new CustomException("Action FAILED \t Storage limit max files:" + StorageArguments.maxFilesInStorage);
                        }
                        if (StorageArguments.usedSpace + localFile.length() > StorageArguments.totalSpace) {
                            throw new CustomException("Action FAILED \t Storage byte size:" + StorageArguments.totalSpace);
                        }
                        if (StorageArguments.restrictedExtensions.contains(FilenameUtils.getExtension(localFile.getName()))) {
                            throw new CustomException("Action FAILED \t Storage unsupported extensions:" + StorageArguments.restrictedExtensions);
                        }
                        FileUtils.copyFileToDirectory(localFile, storageFile);
                        StorageArguments.fileNumberInStorage += 1;
                        StorageArguments.usedSpace = (int) (StorageArguments.usedSpace + localFile.length());
                    }

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new CustomException("Action FAILED \t" + localFile.getAbsolutePath() + "  does not exists");
            }
        }
        return true;
    }

    @Override
    public boolean exportFileObject(String exportStoragePath, String exportLocalPath) {
        File storageFile = new File(StorageArguments.path + exportStoragePath);
        File localFile = new File(exportLocalPath);

        if (!localFile.exists()) {
            throw new CustomException("Action FAILED \t" + localFile.getAbsolutePath() + " does not exists");
        }
        if (!localFile.isDirectory()) {
            throw new CustomException("Action FAILED \t" +localFile.getAbsolutePath() + " is not a folder");
        }
        if (!storageFile.exists()) {
            throw new CustomException("Action FAILED \t"+storageFile.getAbsolutePath() + " does not exists");
        }
        try {
            if (storageFile.isDirectory()) {
                FileUtils.copyDirectoryToDirectory(storageFile, localFile);
            } else if (storageFile.isFile()) {
                FileUtils.copyFileToDirectory(storageFile, localFile);
            }
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /*-----------------------------------------------------------------------------------------------------------*/

    public List<String> sortFiles(List<String> files, TypeSort typeSort) {
        switch (typeSort) {
            case ALPHABETICAL_ASC -> files.sort(String.CASE_INSENSITIVE_ORDER);
            case ALPHABETICAL_DESC -> files.sort(Collections.reverseOrder());
            case CREATED_DATE_ASC -> files.sort((o1, o2) -> {
                long fcd1;
                long fcd2;
                try {
                    fcd1 = (new Date((Files.readAttributes(Paths.get(o1), BasicFileAttributes.class)).creationTime().toMillis())).getTime();
                    fcd2 = (new Date((Files.readAttributes(Paths.get(o2), BasicFileAttributes.class)).creationTime().toMillis())).getTime();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return Long.compare(fcd1, fcd2);
            });
            case CREATED_DATE_DESC -> files.sort((o1, o2) -> {
                long fcd1;
                long fcd2;
                try {
                    fcd1 = (new Date((Files.readAttributes(Paths.get(o1), BasicFileAttributes.class)).creationTime().toMillis())).getTime();
                    fcd2 = (new Date((Files.readAttributes(Paths.get(o2), BasicFileAttributes.class)).creationTime().toMillis())).getTime();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return Long.compare(fcd2, fcd1);
            });
            case MODIFIED_DATE_ASC -> files.sort(Comparator.comparingLong(o -> new File(o).lastModified()));
            case MODIFIED_DATE_DESC ->
                    files.sort((o1, o2) -> Long.compare(new File(o2).lastModified(), new File(o1).lastModified()));
        }
        return files;
    }

    public List<String> filterFilesByExt(List<String> files, TypeFilter typeFilter, String ext) {
        List<String> newFiles = new ArrayList<>();

        if (typeFilter == TypeFilter.FILE_EXTENSION) {
            for (String file : files) {
                if (file.toLowerCase().contains("." + ext.toLowerCase())) {
                    newFiles.add(file);
                }
            }
        }

        return newFiles;
    }

    public List<String> filterFilesByDate(List<String> files, TypeFilter typeFilter, Date beginDate, Date endDate) {
        List<String> newFiles = new ArrayList<>();

        switch (typeFilter) {
            case CREATED_DATE -> {
                for (String file : files) {
                    try {
                        Date fd = new Date((Files.readAttributes(Paths.get(file), BasicFileAttributes.class)).creationTime().toMillis());
                        if (fd.getTime() >= beginDate.getTime() && (fd.getTime() >= endDate.getTime())) {
                            newFiles.add(file);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            case MODIFIED_DATE -> {
                for (String file : files) {
                    File f = new File(file);
                    if (f.lastModified() >= beginDate.getTime() && f.lastModified() <= endDate.getTime()) {
                        newFiles.add(file);
                    }
                }
            }
        }

        return newFiles;
    }

    private static void recSearchFilesInFolders(File[] fo, int index, int level, List<String> files) {
        if (index == fo.length) {
            return;
        }

        if (fo[index].isFile()) {
            files.add(fo[index].getAbsolutePath());
        } else if (fo[index].isDirectory()) {
            recSearchFilesInFolders(Objects.requireNonNull(fo[index].listFiles()), 0, level + 1, files);
        }

        recSearchFilesInFolders(fo, ++index, level, files);
    }

    private static String recFindFileFolder(String fileName, File[] fo, int index, int level) {
        if (index == fo.length) {
            return null;
        }

        if (fo[index].isFile()) {
            if (fo[index].getName().equals(fileName)) {
                return fo[index].getParentFile().getName();
            }
        } else if (fo[index].isDirectory()) {
            recFindFileFolder(fileName, Objects.requireNonNull(fo[index].listFiles()), 0, level + 1);
        }

        recFindFileFolder(fileName, fo, ++index, level);
        return null;
    }

    /*-----------------------------------------------------------------------------------------------------------*/

    @Override
    public List<String> searchFilesInFolder(String folderPath, TypeSort typeSort, TypeFilter typeFilter) {
        List<String> files = new ArrayList<>();
        File folder = new File(StorageArguments.path + folderPath);

        if (folder.exists()) {
            for (File file : Objects.requireNonNull(folder.listFiles())) {
                if (file.isFile()) {
                    files.add(file.getAbsolutePath());
                }
            }
        }

        switch (typeFilter) {
            case FILE_EXTENSION -> {
                String ext = "html";
                files = filterFilesByExt(files, typeFilter, ext);
            }
            case MODIFIED_DATE, CREATED_DATE -> {
                Date d1 = new Date(2022 - 1900, Calendar.OCTOBER, 30);
                Date d2 = new Date(2022 - 1900, Calendar.NOVEMBER, 1);
                files = filterFilesByDate(files, typeFilter, d1, d2);
            }
        }

        if (typeSort != null) {
            files = sortFiles(files, typeSort);
        }

        return files;
    }

    @Override
    public List<String> searchFilesInFolders(String folderPath, TypeSort typeSort, TypeFilter typeFilter) {
        List<String> files = new ArrayList<>();
        File folder = new File(StorageArguments.path + folderPath);

        if (folder.exists()) {
            recSearchFilesInFolders(Objects.requireNonNull(folder.listFiles()), 0, 0, files);
        }

        switch (typeFilter) {
            case FILE_EXTENSION -> {
                String ext = "html";
                files = filterFilesByExt(files, typeFilter, ext);
            }
            case MODIFIED_DATE, CREATED_DATE -> {
                Date d1 = new Date(2022 - 1900, Calendar.OCTOBER, 30);
                Date d2 = new Date(2022 - 1900, Calendar.NOVEMBER, 1);
                files = filterFilesByDate(files, typeFilter, d1, d2);
            }
        }

        if (typeSort != null) {
            files = sortFiles(files, typeSort);
        }

        return files;
    }

    @Override
    public List<String> searchFilesWithExtensionInFolder(String fileExtension, String folderPath, TypeSort typeSort, TypeFilter typeFilter) {
        List<String> files = new ArrayList<>();
        File folder = new File(StorageArguments.path + folderPath);

        if (fileExtension.contains(".")) {
            fileExtension = fileExtension.replace(".", "");
        }

        if (folder.exists()) {
            for (File file : Objects.requireNonNull(folder.listFiles())) {
                if (file.isFile() && FilenameUtils.getExtension(file.getName()).equalsIgnoreCase(fileExtension)) {
                    files.add(file.getAbsolutePath());
                }
            }
        }

        switch (typeFilter) {
            case FILE_EXTENSION -> {
                String ext = "html";
                files = filterFilesByExt(files, typeFilter, ext);
            }
            case MODIFIED_DATE, CREATED_DATE -> {
                Date d1 = new Date(2022 - 1900, Calendar.OCTOBER, 30);
                Date d2 = new Date(2022 - 1900, Calendar.NOVEMBER, 1);
                files = filterFilesByDate(files, typeFilter, d1, d2);
            }
        }

        if (typeSort != null) {
            files = sortFiles(files, typeSort);
        }

        return files;
    }

    @Override
    public List<String> searchFilesWithSubstringInFolder(String fileSubstring, String folderPath, TypeSort typeSort, TypeFilter typeFilter) {
        List<String> files = new ArrayList<>();
        File folder = new File(StorageArguments.path + folderPath);

        if (folder.exists()) {
            for (File file : Objects.requireNonNull(folder.listFiles())) {
                if (file.isFile() && file.getName().toLowerCase().contains(fileSubstring.toLowerCase())) {
                    files.add(file.getAbsolutePath());
                }
            }
        }

        switch (typeFilter) {
            case FILE_EXTENSION -> {
                String ext = "html";
                files = filterFilesByExt(files, typeFilter, ext);
            }
            case MODIFIED_DATE, CREATED_DATE -> {
                Date d1 = new Date(2022 - 1900, Calendar.OCTOBER, 30);
                Date d2 = new Date(2022 - 1900, Calendar.NOVEMBER, 1);
                files = filterFilesByDate(files, typeFilter, d1, d2);
            }
        }

        if (typeSort != null) {
            files = sortFiles(files, typeSort);
        }

        return files;
    }

    @Override
    public boolean existsInFolder(String[] fileName, String folderPath) {
        File folder = new File(StorageArguments.path + folderPath);
        int checker = 0;

        if (folder.exists()) {
            for (String name : fileName) {
                for (File file : Objects.requireNonNull(folder.listFiles())) {
                    if (file.isFile() && name.equals(file.getName())) {
                        checker++;
                    }
                }
            }
        }

        return checker == fileName.length;
    }

    @Override
    public String findFileFolder(String fileName) {
        return recFindFileFolder(fileName, Objects.requireNonNull(new File(StorageArguments.path).listFiles()), 0, 0);
    }

    @Override
    public List<String> searchModifiedFilesInFolder(Date beginDate, Date endDate, String folderPath, TypeSort typeSort, TypeFilter typeFilter) {
        List<String> files = new ArrayList<>();
        File folder = new File(StorageArguments.path + folderPath);

        if (folder.exists()) {
            for (File file : Objects.requireNonNull(folder.listFiles())) {
                if (file.isFile() && (file.lastModified() >= beginDate.getTime() && file.lastModified() <= endDate.getTime())) {
                    files.add(file.getAbsolutePath());
                }
            }
        }

        switch (typeFilter) {
            case FILE_EXTENSION -> {
                String ext = "html";
                files = filterFilesByExt(files, typeFilter, ext);
            }
            case MODIFIED_DATE, CREATED_DATE -> {
                Date d1 = new Date(2022 - 1900, Calendar.OCTOBER, 30);
                Date d2 = new Date(2022 - 1900, Calendar.NOVEMBER, 1);
                files = filterFilesByDate(files, typeFilter, d1, d2);
            }
        }

        if (typeSort != null) {
            files = sortFiles(files, typeSort);
        }

        return files;
    }

}