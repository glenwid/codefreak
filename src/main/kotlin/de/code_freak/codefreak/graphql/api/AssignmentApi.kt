package de.code_freak.codefreak.graphql.api

import com.expediagroup.graphql.annotations.GraphQLID
import com.expediagroup.graphql.annotations.GraphQLIgnore
import com.expediagroup.graphql.annotations.GraphQLName
import com.expediagroup.graphql.spring.operations.Mutation
import com.expediagroup.graphql.spring.operations.Query
import de.code_freak.codefreak.auth.Authority
import de.code_freak.codefreak.auth.Authorization
import de.code_freak.codefreak.auth.hasAuthority
import de.code_freak.codefreak.entity.Assignment
import de.code_freak.codefreak.entity.AssignmentStatus
import de.code_freak.codefreak.graphql.BaseDto
import de.code_freak.codefreak.graphql.BaseResolver
import de.code_freak.codefreak.graphql.ResolverContext
import de.code_freak.codefreak.service.AssignmentService
import de.code_freak.codefreak.service.SubmissionService
import de.code_freak.codefreak.service.TaskService
import de.code_freak.codefreak.util.FrontendUtil
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@GraphQLName("Assignment")
class AssignmentDto(@GraphQLIgnore val entity: Assignment, ctx: ResolverContext) : BaseDto(ctx) {

  @GraphQLID
  val id = entity.id
  val title = entity.title
  val owner by lazy { UserDto(entity.owner, ctx) }
  val createdAt = entity.createdAt
  val deadline = entity.deadline
  val status by lazy { entity.status }
  val active = entity.active
  val openFrom = entity.openFrom
  val tasks by lazy { entity.tasks.map { TaskDto(it, ctx) } }
  val editable by lazy { entity.isEditable(authorization) }
  val deletable by lazy {
    authorization.currentUser.hasAuthority(Authority.ROLE_ADMIN) || (authorization.isCurrentUser(entity.owner) &&
        (status != AssignmentStatus.OPEN))
  }

  val submissionCsvUrl by lazy { FrontendUtil.getUriBuilder().path("/api/assignments/$id/submissions.csv").build().toUriString() }
  val submissions by lazy {
    authorization.requireAuthority(Authority.ROLE_TEACHER)
    serviceAccess.getService(SubmissionService::class)
        .findSubmissionsOfAssignment(id)
        .map { SubmissionDto(it, ctx) }
  }
}

@Component
class AssignmentQuery : BaseResolver(), Query {

  @Transactional
  @Secured(Authority.ROLE_STUDENT)
  fun assignments(): List<AssignmentDto> = context {
    val assignmentService = serviceAccess.getService(AssignmentService::class)
    val user = FrontendUtil.getCurrentUser()
    val assignments = when {
      authorization.currentUser.hasAuthority(Authority.ROLE_ADMIN)
          -> assignmentService.findAllAssignments()
      authorization.currentUser.hasAuthority(Authority.ROLE_TEACHER)
          -> assignmentService.findAssignmentsByOwner(authorization.currentUser)
      else -> assignmentService.findAllAssignmentsForUser(user.id)
    }
    assignments.map { AssignmentDto(it, this) }
  }

  @Transactional
  fun assignment(id: UUID): AssignmentDto = context {
    serviceAccess.getService(AssignmentService::class)
        .findAssignment(id)
        .let { AssignmentDto(it, this) }
  }
}

@Component
class AssignmentMutation : BaseResolver(), Mutation {

  @Secured(Authority.ROLE_TEACHER)
  fun createAssignment(): AssignmentDto = context {
    serviceAccess.getService(AssignmentService::class).createEmptyAssignment(authorization.currentUser).let { AssignmentDto(it, this) }
  }

  fun deleteAssignment(id: UUID): Boolean = context {
    val assignment = serviceAccess.getService(AssignmentService::class).findAssignment(id)
    authorization.requireAuthorityIfNotCurrentUser(assignment.owner, Authority.ROLE_ADMIN)
    require(assignment.status != AssignmentStatus.OPEN) { "Assignment must not be open" }
    serviceAccess.getService(AssignmentService::class).deleteAssignment(assignment.id)
    true
  }

  fun addTasksToAssignment(assignmentId: UUID, taskIds: Array<UUID>): Boolean = context {
    val assignment = serviceAccess.getService(AssignmentService::class).findAssignment(assignmentId)
    require(assignment.isEditable(authorization)) { "Assignment is not editable" }
    val tasks = taskIds.map { serviceAccess.getService(TaskService::class).findTask(it) }
    tasks.forEach {
      authorization.requireAuthorityIfNotCurrentUser(it.owner, Authority.ROLE_ADMIN)
    }
    serviceAccess.getService(AssignmentService::class).addTasksToAssignment(assignment, tasks)
    true
  }
}

fun Assignment.isEditable(authorization: Authorization) = authorization.currentUser.hasAuthority(Authority.ROLE_ADMIN) ||
    (authorization.isCurrentUser(owner) &&
    (status < AssignmentStatus.OPEN))
