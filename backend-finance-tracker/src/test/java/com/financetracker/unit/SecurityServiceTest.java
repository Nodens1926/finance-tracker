package com.financetracker.unit;

import com.financetracker.entity.Role;
import com.financetracker.entity.Transaction;
import com.financetracker.entity.TransactionAttachment;
import com.financetracker.entity.User;
import com.financetracker.repository.TransactionAttachmentRepository;
import com.financetracker.repository.TransactionRepository;
import com.financetracker.repository.UserRepository;
import com.financetracker.security.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionAttachmentRepository attachmentRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private SecurityService securityService;

    private User testUser;
    private User adminUser;
    private User managerUser;
    private Transaction testTransaction;
    private TransactionAttachment testAttachment;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("user");
        testUser.setRoles(Set.of(Role.ROLE_USER));

        adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUsername("admin");
        adminUser.setRoles(Set.of(Role.ROLE_ADMIN));

        managerUser = new User();
        managerUser.setId(3L);
        managerUser.setUsername("manager");
        managerUser.setRoles(Set.of(Role.ROLE_MANAGER));

        testTransaction = new Transaction();
        testTransaction.setId(100L);
        testTransaction.setUser(testUser);

        testAttachment = new TransactionAttachment();
        testAttachment.setId(200L);
        testAttachment.setTransaction(testTransaction);
        testAttachment.setUploadedBy(testUser);
    }

    private void mockAuthenticatedUser(User user) {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(user.getUsername());
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
    }

    @Test
    void isCurrentUser_withOwnId_shouldReturnTrue() {
        // given
        mockAuthenticatedUser(testUser);

        // when
        boolean result = securityService.isCurrentUser(1L);

        // then
        assertTrue(result);
    }

    @Test
    void isCurrentUser_withOtherId_shouldReturnFalse() {
        // given
        mockAuthenticatedUser(testUser);

        // when
        boolean result = securityService.isCurrentUser(999L);

        // then
        assertFalse(result);
    }

    @Test
    void hasRole_withCorrectRole_shouldReturnTrue() {
        // given
        mockAuthenticatedUser(testUser);

        // when
        boolean result = securityService.hasRole(Role.ROLE_USER);

        // then
        assertTrue(result);
    }

    @Test
    void hasRole_withWrongRole_shouldReturnFalse() {
        // given
        mockAuthenticatedUser(testUser);

        // when
        boolean result = securityService.hasRole(Role.ROLE_ADMIN);

        // then
        assertFalse(result);
    }

    @Test
    void canAccessUserData_asOwner_shouldReturnTrue() {
        // given
        mockAuthenticatedUser(testUser);

        // when
        boolean result = securityService.canAccessUserData(1L);

        // then
        assertTrue(result);
    }

    @Test
    void canAccessUserData_asAdmin_shouldReturnTrue() {
        // given
        mockAuthenticatedUser(adminUser);

        // when
        boolean result = securityService.canAccessUserData(1L);

        // then
        assertTrue(result);
    }

    @Test
    void canAccessUserData_asManager_shouldReturnTrue() {
        // given
        mockAuthenticatedUser(managerUser);

        // when
        boolean result = securityService.canAccessUserData(1L);

        // then
        assertTrue(result);
    }

    @Test
    void canAccessUserData_asOtherUser_shouldReturnFalse() {
        // given
        User otherUser = new User();
        otherUser.setId(5L);
        otherUser.setRoles(Set.of(Role.ROLE_USER));
        mockAuthenticatedUser(otherUser);

        // when
        boolean result = securityService.canAccessUserData(1L);

        // then
        assertFalse(result);
    }

    @Test
    void canModifyUserData_asOwner_shouldReturnTrue() {
        // given
        mockAuthenticatedUser(testUser);
        when(transactionRepository.findById(100L)).thenReturn(Optional.of(testTransaction));

        // when
        boolean result = securityService.canModifyUserData(100L);

        // then
        assertTrue(result);
    }

    @Test
    void canModifyUserData_asAdmin_shouldReturnTrue() {
        // given
        mockAuthenticatedUser(adminUser);

        // when
        boolean result = securityService.canModifyUserData(100L);

        // then
        assertTrue(result);
    }

    @Test
    void canModifyUserData_asManager_withOwnTransaction_shouldReturnTrue() {
        // given
        Transaction managerTransaction = new Transaction();
        managerTransaction.setId(101L);
        managerTransaction.setUser(managerUser);

        mockAuthenticatedUser(managerUser);
        when(transactionRepository.findById(101L)).thenReturn(Optional.of(managerTransaction));

        // when
        boolean result = securityService.canModifyUserData(101L);

        // then
        assertTrue(result);
    }

    @Test
    void canModifyUserData_asManager_withOthersTransaction_shouldReturnFalse() {
        // given
        mockAuthenticatedUser(managerUser);
        when(transactionRepository.findById(100L)).thenReturn(Optional.of(testTransaction));

        // when
        boolean result = securityService.canModifyUserData(100L);

        // then
        assertFalse(result);
    }

    @Test
    void canAccessAttachment_asOwner_shouldReturnTrue() {
        // given
        mockAuthenticatedUser(testUser);
        when(attachmentRepository.findById(200L)).thenReturn(Optional.of(testAttachment));

        // when
        boolean result = securityService.canAccessAttachment(200L);

        // then
        assertTrue(result);
    }

    @Test
    void canAccessAttachment_asAdmin_shouldReturnTrue() {
        mockAuthenticatedUser(adminUser);
        // Админу не нужно искать attachment - он всегда может
        // УБЕРИ строку: when(attachmentRepository.findById(200L))...

        boolean result = securityService.canAccessAttachment(200L);

        assertTrue(result);
    }

    @Test
    void canModifyAttachment_asOwner_shouldReturnTrue() {
        // given
        mockAuthenticatedUser(testUser);
        when(attachmentRepository.findById(200L)).thenReturn(Optional.of(testAttachment));

        // when
        boolean result = securityService.canModifyAttachment(200L);

        // then
        assertTrue(result);
    }

    @Test
    void canModifyAttachment_asAdmin_shouldReturnTrue() {
        mockAuthenticatedUser(adminUser);
        // Админу не нужен мок

        boolean result = securityService.canModifyAttachment(200L);

        assertTrue(result);
    }

    @Test
    void canModifyAttachment_asNonOwner_shouldReturnFalse() {
        // given
        User otherUser = new User();
        otherUser.setId(5L);
        otherUser.setRoles(Set.of(Role.ROLE_USER));
        mockAuthenticatedUser(otherUser);
        when(attachmentRepository.findById(200L)).thenReturn(Optional.of(testAttachment));

        // when
        boolean result = securityService.canModifyAttachment(200L);

        // then
        assertFalse(result);
    }
}