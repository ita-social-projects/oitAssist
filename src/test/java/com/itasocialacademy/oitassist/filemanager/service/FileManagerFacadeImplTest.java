package com.itasocialacademy.oitassist.filemanager.service;

import com.itasocialacademy.oitassist.filemanager.api.dto.FileDetailsDTO;
import com.itasocialacademy.oitassist.filemanager.api.dto.StorageContext;
import com.itasocialacademy.oitassist.filemanager.dao.enums.FileRole;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.filemanager.dto.request.FileUploadRequestDto;
import com.itasocialacademy.oitassist.filemanager.dto.request.UpdateFileRoleRequestDto;
import com.itasocialacademy.oitassist.filemanager.service.interfaces.FileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileManagerFacadeImplTest {

    @Mock
    private FileService fileService;

    @InjectMocks
    private FileManagerFacadeImpl fileManagerFacade;

    @Test
    void uploadFiles_shouldConstructRequestAndDelegateToFileService() {
        MultipartFile file = mock(MultipartFile.class);
        List<MultipartFile> files = List.of(file);
        StorageContext ctx = StorageContext.forTask("Test Task");
        List<FileDetailsDTO> expected =
            List.of(new FileDetailsDTO(1L, "file.txt", "text/plain", 100L, "PROBLEM", "url"));

        when(fileService.uploadToFileDetails(eq(files), any(FileUploadRequestDto.class))).thenReturn(expected);

        List<FileDetailsDTO> result = fileManagerFacade.uploadFiles(
            files, RelatedEntityType.TASK, 1L, FileRole.PROBLEM, ctx);

        assertThat(result).isEqualTo(expected);

        ArgumentCaptor<FileUploadRequestDto> captor = ArgumentCaptor.forClass(FileUploadRequestDto.class);
        verify(fileService).uploadToFileDetails(eq(files), captor.capture());
        FileUploadRequestDto dto = captor.getValue();
        assertThat(dto.getRelatedEntityType()).isEqualTo(RelatedEntityType.TASK);
        assertThat(dto.getRelatedEntityId()).isEqualTo(1L);
        assertThat(dto.getFileRole()).isEqualTo(FileRole.PROBLEM);
        assertThat(dto.getStorageContext()).isEqualTo(ctx);
    }

    @Test
    void createTaskReference_shouldDelegateToFileService() {
        StorageContext ctx = StorageContext.forAssignment("Comp", "Stage", "Tour", "Task");

        fileManagerFacade.createTaskReference(ctx);

        verify(fileService).createTaskReference(ctx);
    }

    @Test
    void deleteTaskReference_shouldDelegateToFileService() {
        StorageContext ctx = StorageContext.forAssignment("Comp", "Stage", "Tour", "Task");

        fileManagerFacade.deleteTaskReference(ctx);

        verify(fileService).deleteTaskReference(ctx);
    }

    @Test
    void getFilesByEntity_shouldDelegateToFileService() {
        Set<FileRole> roles = Set.of(FileRole.PROBLEM);
        when(fileService.getFilesByEntity(RelatedEntityType.TASK, 1L, roles)).thenReturn(List.of());

        List<FileDetailsDTO> result = fileManagerFacade.getFilesByEntity(RelatedEntityType.TASK, 1L, roles);

        assertThat(result).isNotNull();
        verify(fileService).getFilesByEntity(RelatedEntityType.TASK, 1L, roles);
    }

    @Test
    void getFilesByEntities_shouldDelegateToFileService() {
        Set<FileRole> roles = Set.of(FileRole.PROBLEM);
        when(fileService.getFilesByEntities(RelatedEntityType.TASK, List.of(1L), roles)).thenReturn(Map.of());

        Map<Long, List<FileDetailsDTO>> result =
            fileManagerFacade.getFilesByEntities(RelatedEntityType.TASK, List.of(1L), roles);

        assertThat(result).isNotNull();
        verify(fileService).getFilesByEntities(RelatedEntityType.TASK, List.of(1L), roles);
    }

    @Test
    void detachAllFilesByEntity_shouldDelegateToFileService() {
        fileManagerFacade.detachAllFilesByEntity(RelatedEntityType.TASK, 1L, 100L);

        verify(fileService).detachAllFilesByEntityId(RelatedEntityType.TASK, 1L, 100L);
    }

    @Test
    void detachFiles_shouldDelegateToFileService() {
        fileManagerFacade.detachFiles(RelatedEntityType.TASK, 1L, List.of(10L), 100L);

        verify(fileService).detachFiles(RelatedEntityType.TASK, 1L, List.of(10L), 100L);
    }

    @Test
    void detachFilesForMultiOwnerEntity_shouldDelegateToFileService() {
        fileManagerFacade.detachFilesForMultiOwnerEntity(RelatedEntityType.TASK, 1L, List.of(10L));

        verify(fileService).detachFilesForMultiOwnerEntity(RelatedEntityType.TASK, 1L, List.of(10L));
    }

    @Test
    void updateFileRole_shouldDelegateToFileService() {
        fileManagerFacade.updateFileRole(1L, FileRole.SOLUTION);

        verify(fileService).updateRoleGeneral(eq(1L), any(UpdateFileRoleRequestDto.class));
    }

    @Test
    void updateRoleForMultiOwnerEntity_shouldDelegateToFileService() {
        fileManagerFacade.updateRoleForMultiOwnerEntity(1L, FileRole.SOLUTION, RelatedEntityType.TASK, 2L);

        verify(fileService).updateRoleForMultiOwnerEntity(1L, FileRole.SOLUTION, RelatedEntityType.TASK, 2L);
    }
}
