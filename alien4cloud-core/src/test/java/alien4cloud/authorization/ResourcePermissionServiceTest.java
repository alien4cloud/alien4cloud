package alien4cloud.authorization;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.EnvironmentType;
import alien4cloud.security.AbstractSecurityEnabledResource;
import alien4cloud.security.ISecurityEnabledResource;
import alien4cloud.security.Permission;
import alien4cloud.security.Subject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationEventPublisher;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ResourcePermissionServiceTest {

    private ResourcePermissionService service;

    @Mock
    private IGenericSearchDAO alienDAO;

    @Mock
    private ResourcePermissionService.IResourceSaver resourceSaver;

    @Captor
    private ArgumentCaptor<ISecurityEnabledResource> resourceSecuredCaptor;

    @Mock
    private ISecurityEnabledResource resourceSecured;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private ApplicationEnvironmentService applicationEnvironmentService;

    @Before
    public void setUp() throws Exception {
        service = new ResourcePermissionService(alienDAO, null, null, applicationEnvironmentService, publisher);
    }

    @Test
    public void when_permission_revoked_the_resource_is_saved() throws Exception {
        service.revokePermission(resourceSecured, resourceSaver, Subject.APPLICATION);
        verify(resourceSaver).save(resourceSecured);
    }

    @Test
    public void when_many_app_env_permission_are_revoked_the_resource_is_saved_only_one_time() throws Exception {
        // Given
        resourceSecured = new AbstractSecurityEnabledResource() {
            @Override
            public String getId() {
                return "id";
            }
        };

        HashSet<Permission> permissions = new HashSet<>();
        permissions.add(Permission.ADMIN);
        resourceSecured.addPermissions(Subject.APPLICATION, "subject1", new HashSet<>(permissions));
        resourceSecured.addPermissions(Subject.ENVIRONMENT, "subject1_1", new HashSet<>(permissions));
        resourceSecured.addPermissions(Subject.ENVIRONMENT, "subject1_2", new HashSet<>(permissions));
        resourceSecured.addPermissions(Subject.ENVIRONMENT_TYPE, "subject_env_1_1", new HashSet<>(permissions));

        ApplicationEnvironment ae1 = new ApplicationEnvironment();
        ae1.setId("subject1_1");

        ApplicationEnvironment ae2 = new ApplicationEnvironment();
        ae2.setId("subject1_2");

        Mockito.when(applicationEnvironmentService.getByApplicationId("subject1")).thenReturn(new ApplicationEnvironment[] { ae1, ae2 });

        // When
        service.revokeAuthorizedEnvironmentsAndEnvironmentTypesPerApplication((AbstractSecurityEnabledResource) resourceSecured, new String[] { "subject1" }, new String[] {
                "subject1_1", "subject1_2" }, new String[] {"subject_env_1_1" });

        // Then
        verify(alienDAO).save(resourceSecuredCaptor.capture());
    }

    @Test
    public void check_revoked_permissions_have_been_removed_from_resource() {
        // Given
        resourceSecured = new AbstractSecurityEnabledResource() {
            @Override
            public String getId() {
                return "id";
            }
        };

        HashSet<Permission> permissions = new HashSet<>();
        permissions.add(Permission.READ);
        permissions.add(Permission.ADMIN);
        resourceSecured.addPermissions(Subject.APPLICATION, "subject1", new HashSet<>(permissions));
        resourceSecured.addPermissions(Subject.APPLICATION, "subject3", new HashSet<>(permissions));

        // When
        service.revokePermission(resourceSecured, resourceSaver, Subject.APPLICATION, "subject1", "subject2");

        // Then
        verify(resourceSaver).save(resourceSecuredCaptor.capture());

        ISecurityEnabledResource resourceSecuredSaved = resourceSecuredCaptor.getValue();
        assertThat(resourceSecuredSaved.getPermissions(Subject.APPLICATION, "subject1")).containsExactly(Permission.READ);
        assertThat(resourceSecuredSaved.getPermissions(Subject.APPLICATION, "subject2")).isEmpty();
        assertThat(resourceSecuredSaved.getPermissions(Subject.APPLICATION, "subject3")).containsExactlyInAnyOrder(Permission.ADMIN, Permission.READ);
    }

    @Test
    public void when_permission_added_at_application_level_remove_any_permissions_hidden_at_lower_level() {
        // Given
        resourceSecured = new AbstractSecurityEnabledResource() {
            @Override
            public String getId() {
                return "id";
            }
        };

        HashSet<Permission> permissions = new HashSet<>();
        permissions.add(Permission.ADMIN);
        resourceSecured.addPermissions(Subject.APPLICATION, "subject1", new HashSet<>(permissions));
        resourceSecured.addPermissions(Subject.ENVIRONMENT, "subject1_1", new HashSet<>(permissions));
        resourceSecured.addPermissions(Subject.ENVIRONMENT, "subject1_2", new HashSet<>(permissions));
        resourceSecured.addPermissions(Subject.ENVIRONMENT_TYPE, "subject1:INTEGRATION_TESTS", new HashSet<>(permissions));

        ApplicationEnvironment ae1 = new ApplicationEnvironment();
        ae1.setId("subject1_1");
        ae1.setEnvironmentType(EnvironmentType.INTEGRATION_TESTS);

        ApplicationEnvironment ae2 = new ApplicationEnvironment();
        ae2.setId("subject1_2");
        ae2.setEnvironmentType(EnvironmentType.INTEGRATION_TESTS);

        Mockito.when(applicationEnvironmentService.getByApplicationId("subject1")).thenReturn(new ApplicationEnvironment[] { ae1, ae2 });

        // When
        service.grantAuthorizedEnvironmentsAndEnvTypesPerApplication((AbstractSecurityEnabledResource) resourceSecured, new String[] { "subject1" }, new String[] {
                "subject1_1", "subject1_2" }, new String[] {"subject_env_1_1" });

        // Then
        verify(alienDAO).save(resourceSecuredCaptor.capture());

        assertThat(resourceSecuredCaptor.getValue().getPermissions(Subject.APPLICATION, "subject1")).containsExactly(Permission.ADMIN);
        assertThat(resourceSecuredCaptor.getValue().getPermissions(Subject.ENVIRONMENT, "subject1_1")).isEmpty();
        assertThat(resourceSecuredCaptor.getValue().getPermissions(Subject.ENVIRONMENT, "subject1_2")).isEmpty();
        assertThat(resourceSecuredCaptor.getValue().getPermissions(Subject.ENVIRONMENT_TYPE, "subject1:INTEGRATION_TESTS")).isEmpty();
    }

    @Test
    public void when_permission_added_at_application_level_unrelated_environment_and_env_type_are_unmodified() {
        // Given
        resourceSecured = new AbstractSecurityEnabledResource() {
            @Override
            public String getId() {
                return "id";
            }
        };

        HashSet<Permission> permissions = new HashSet<>();
        permissions.add(Permission.ADMIN);
        resourceSecured.addPermissions(Subject.APPLICATION, "subject1", new HashSet<>(permissions));
        resourceSecured.addPermissions(Subject.ENVIRONMENT, "subject1_1", new HashSet<>(permissions));
        resourceSecured.addPermissions(Subject.ENVIRONMENT, "subject2_1", new HashSet<>(permissions));
        resourceSecured.addPermissions(Subject.ENVIRONMENT_TYPE, "subject1:INTEGRATION_TESTS", new HashSet<>(permissions));

        ApplicationEnvironment ae1 = new ApplicationEnvironment();
        ae1.setId("subject1_1");
        ae1.setEnvironmentType(EnvironmentType.INTEGRATION_TESTS);

        ApplicationEnvironment ae2 = new ApplicationEnvironment();
        ae2.setId("subject2_1");
        ae2.setEnvironmentType(EnvironmentType.INTEGRATION_TESTS);

        Mockito.when(applicationEnvironmentService.getByApplicationId("subject1")).thenReturn(new ApplicationEnvironment[] { ae1 });

        // When
        service.grantAuthorizedEnvironmentsAndEnvTypesPerApplication((AbstractSecurityEnabledResource) resourceSecured, new String[] { "subject1" }, new String[] {
                "subject1_1", "subject2_1" }, new String[] { EnvironmentType.INTEGRATION_TESTS.toString() });

        // Then
        verify(alienDAO).save(resourceSecuredCaptor.capture());

        assertThat(resourceSecuredCaptor.getValue().getPermissions(Subject.APPLICATION, "subject1")).containsExactly(Permission.ADMIN);
        assertThat(resourceSecuredCaptor.getValue().getPermissions(Subject.ENVIRONMENT, "subject1_1")).isEmpty();
        assertThat(resourceSecuredCaptor.getValue().getPermissions(Subject.ENVIRONMENT, "subject2_1")).containsExactly(Permission.ADMIN);
        assertThat(resourceSecuredCaptor.getValue().getPermissions(Subject.ENVIRONMENT_TYPE, EnvironmentType.INTEGRATION_TESTS.toString())).containsExactly(Permission.ADMIN);
    }

}