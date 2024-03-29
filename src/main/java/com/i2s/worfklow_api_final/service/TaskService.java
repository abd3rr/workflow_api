package com.i2s.worfklow_api_final.service;

import com.i2s.worfklow_api_final.dto.FeedbackDTO;
import com.i2s.worfklow_api_final.dto.MethodExecutionDTO;
import com.i2s.worfklow_api_final.dto.TaskDTO;
import com.i2s.worfklow_api_final.dto.UserParameterDTO;
import com.i2s.worfklow_api_final.enums.ExecutionStatus;
import com.i2s.worfklow_api_final.enums.TaskStatus;
import com.i2s.worfklow_api_final.model.*;
import com.i2s.worfklow_api_final.repository.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaskService {
    private final TaskRepository taskRepository;
    private final FileRepository fileRepository;
    private final MethodRepository methodRepository;
    private final JobRepository jobRepository;
    private final StepRepository stepRepository;
    private final ActionService actionService;
    private final MethodExecutionRepository methodExecutionRepository;
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final NotificationRepository notificationRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public TaskService(TaskRepository taskRepository, FileRepository fileRepository, MethodRepository methodRepository, JobRepository jobRepository, StepRepository stepRepository, ActionService actionService, MethodExecutionRepository methodExecutionRepository, FeedbackRepository feedbackRepository, UserRepository userRepository, ProjectRepository projectRepository, NotificationRepository notificationRepository, ModelMapper modelMapper) {
        this.taskRepository = taskRepository;
        this.fileRepository = fileRepository;
        this.methodRepository = methodRepository;
        this.jobRepository = jobRepository;
        this.stepRepository = stepRepository;
        this.actionService = actionService;
        this.methodExecutionRepository = methodExecutionRepository;
        this.feedbackRepository = feedbackRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.notificationRepository = notificationRepository;
        this.modelMapper = modelMapper;
    }

    public List<TaskDTO> getAllTasks() {
        return taskRepository.findAll().stream().map(task -> {
            TaskDTO taskDTO = modelMapper.map(task, TaskDTO.class);
            taskDTO.setStatus(task.getStatus());
            taskDTO.setFileIds(task.getFiles().stream().map(File::getId).collect(Collectors.toList()));
            taskDTO.setMethodIds(task.getMethods().stream().map(Method::getId).collect(Collectors.toList()));
            taskDTO.setAssignedJobIds(task.getAssignedJobs().stream().map(Job::getId).collect(Collectors.toList()));
            taskDTO.setParentTaskIds(task.getParentTasks().stream().map(Task::getId).collect(Collectors.toList()));
            taskDTO.setChildTaskIds(task.getChildTasks().stream().map(Task::getId).collect(Collectors.toList()));
            return taskDTO;
        }).collect(Collectors.toList());
    }

    public Optional<TaskDTO> getTaskById(long id) {
        return taskRepository.findById(id).map(task -> {
            TaskDTO taskDTO = modelMapper.map(task, TaskDTO.class);
            taskDTO.setStatus(task.getStatus());
            taskDTO.setFileIds(task.getFiles().stream().map(File::getId).collect(Collectors.toList()));
            taskDTO.setMethodIds(task.getMethods().stream().map(Method::getId).collect(Collectors.toList()));
            taskDTO.setAssignedJobIds(task.getAssignedJobs().stream().map(Job::getId).collect(Collectors.toList()));
            taskDTO.setParentTaskIds(task.getParentTasks().stream().map(Task::getId).collect(Collectors.toList()));
            taskDTO.setChildTaskIds(task.getChildTasks().stream().map(Task::getId).collect(Collectors.toList()));

            return taskDTO;
        });
    }

    public TaskDTO createTask(@Valid TaskDTO taskDTO) {
        Task task = modelMapper.map(taskDTO, Task.class);
        if (taskDTO.getFileIds() != null && !taskDTO.getFileIds().isEmpty()) {
            task.setFiles(taskDTO.getFileIds().stream().map(fileId -> fileRepository.findById(fileId).orElseThrow(() -> new EntityNotFoundException("File with ID " + fileId + " not found."))).collect(Collectors.toList()));
        }
        if (taskDTO.getMethodIds() != null && !taskDTO.getMethodIds().isEmpty()) {
            task.setMethods(taskDTO.getMethodIds().stream().map(methodId -> methodRepository.findById(methodId).orElseThrow(() -> new EntityNotFoundException("Method with ID " + methodId + " not found."))).collect(Collectors.toList()));
        }

        if (taskDTO.getParentTaskIds() != null && !taskDTO.getParentTaskIds().isEmpty()) {
            List<Task> parentTasks = taskDTO.getParentTaskIds().stream().map(parentTaskId -> taskRepository.findById(parentTaskId).orElseThrow(() -> new EntityNotFoundException("Parent Task with ID " + parentTaskId + " not found."))).collect(Collectors.toList());
            task.setParentTasks(parentTasks);
            parentTasks.forEach(parentTask -> {
                List<Task> childTasks = parentTask.getChildTasks();
                childTasks.add(task);
                parentTask.setChildTasks(childTasks);
                taskRepository.save(parentTask);
            });
        }

        task.setAssignedJobs(taskDTO.getAssignedJobIds().stream().map(jobId -> jobRepository.findById(jobId).orElseThrow(() -> new EntityNotFoundException("Assigned Job with ID " + jobId + " not found."))).collect(Collectors.toList()));

        Task savedTask = taskRepository.save(task);

        if (taskDTO.getMethodIds() != null && !taskDTO.getMethodIds().isEmpty()) {
            List<MethodExecution> methodExecutions = new ArrayList<>();
            for (Long methodId : taskDTO.getMethodIds()) {
                Method method = methodRepository.findById(methodId).orElseThrow(() -> new EntityNotFoundException("Method with ID " + methodId + " not found."));
                MethodExecution methodExecution = new MethodExecution();
                methodExecution.setTask(savedTask); // Use the savedTask instance here
                methodExecution.setMethod(method);
                methodExecution.setStatus(ExecutionStatus.PENDING);
                methodExecutions.add(methodExecution);
            }
            // Save all MethodExecution instances
            methodExecutionRepository.saveAll(methodExecutions);
            savedTask.setMethodExecutions(methodExecutions);
        }

        TaskDTO savedTaskDTO = modelMapper.map(savedTask, TaskDTO.class);

        savedTaskDTO.setStatus(savedTask.getStatus());
        savedTaskDTO.setAssignedJobIds(savedTask.getAssignedJobs().stream().map(Job::getId).collect(Collectors.toList()));
        savedTaskDTO.setFileIds(savedTask.getFiles().stream().map(File::getId).collect(Collectors.toList()));
        savedTaskDTO.setMethodIds(savedTask.getMethods().stream().map(Method::getId).collect(Collectors.toList()));
        savedTaskDTO.setParentTaskIds(savedTask.getParentTasks().stream().map(Task::getId).collect(Collectors.toList()));
        savedTaskDTO.setMethodExecutions(savedTask.getMethodExecutions().stream().map(methodExecution -> {
            MethodExecutionDTO methodExecutionDTO = modelMapper.map(methodExecution, MethodExecutionDTO.class);
            return methodExecutionDTO;

        }).collect(Collectors.toList()));

        return savedTaskDTO;
    }


    public void deleteTask(long id) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Task with ID " + id + " not found."));
        taskRepository.deleteById(id);
    }

    public List<TaskDTO> getTasksByStep(long stepId) {
        Step step = stepRepository.findById(stepId).orElseThrow(() -> new EntityNotFoundException("Step with ID " + stepId + " not found."));
        List<Task> tasks = taskRepository.findByStep(step);
        return tasks.stream().map(task -> {
            TaskDTO taskDTO = modelMapper.map(task, TaskDTO.class);
            taskDTO.setStatus(task.getStatus());
            taskDTO.setFileIds(task.getFiles().stream().map(File::getId).collect(Collectors.toList()));
            taskDTO.setMethodIds(task.getMethods().stream().map(Method::getId).collect(Collectors.toList()));
            taskDTO.setAssignedJobIds(task.getAssignedJobs().stream().map(Job::getId).collect(Collectors.toList()));
            taskDTO.setParentTaskIds(task.getParentTasks().stream().map(Task::getId).collect(Collectors.toList()));
            taskDTO.setChildTaskIds(task.getChildTasks().stream().map(Task::getId).collect(Collectors.toList()));
            return taskDTO;
        }).collect(Collectors.toList());
    }

    public List<TaskDTO> getTasksByProject(long projectId) {
        List<Task> tasks = taskRepository.findByProjectId(projectId);

        return tasks.stream().map(task -> {
            TaskDTO taskDTO = modelMapper.map(task, TaskDTO.class);
            taskDTO.setStatus(task.getStatus());
            taskDTO.setFileIds(task.getFiles().stream().map(File::getId).collect(Collectors.toList()));
            taskDTO.setMethodIds(task.getMethods().stream().map(Method::getId).collect(Collectors.toList()));
            taskDTO.setAssignedJobIds(task.getAssignedJobs().stream().map(Job::getId).collect(Collectors.toList()));
            taskDTO.setParentTaskIds(task.getParentTasks().stream().map(Task::getId).collect(Collectors.toList()));
            taskDTO.setChildTaskIds(task.getChildTasks().stream().map(Task::getId).collect(Collectors.toList()));
            return taskDTO;
        }).collect(Collectors.toList());
    }

    public TaskDTO updateTaskStatus(long id, @Valid TaskStatus newStatus) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Task with ID " + id + " not found."));
        task.setStatus(newStatus);
        Task updatedTask = taskRepository.save(task);
        TaskDTO updatedTaskDTO = modelMapper.map(updatedTask, TaskDTO.class);

        if (newStatus == TaskStatus.FINISHED) {
            startNextTask(updatedTaskDTO);
        }

        return updatedTaskDTO;
    }


    public Optional<String> getTaskNameById(long id) {
        return taskRepository.findById(id).map(Task::getTaskName);
    }

    public TaskDTO startNextTask(@Valid TaskDTO finishedTaskDTO) {
        Task finishedTask = taskRepository.findById(finishedTaskDTO.getId()).orElseThrow(() -> new EntityNotFoundException("Task with ID " + finishedTaskDTO.getId() + " not found."));
        List<Task> childTasks = finishedTask.getChildTasks();
        TaskDTO startedTaskDTO = null;

        if (childTasks != null && !childTasks.isEmpty()) {
            for (Task childTask : childTasks) {
                if (childTask.getStatus() == TaskStatus.PENDING) {
                    childTask.setStatus(TaskStatus.STARTING);
                    childTask.setStartedAt(LocalDateTime.now());
                    createNotificationsForTask(childTask, "The task '" + childTask.getTaskName() + "' has started.");

                    Task startedTask = taskRepository.save(childTask);
                    startedTaskDTO = modelMapper.map(startedTask, TaskDTO.class);
                    break;
                }
            }
        }

        return startedTaskDTO;

    }

    public List<TaskDTO> startInitialTasks(long projectId) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new EntityNotFoundException("Project with ID " + projectId + " not found."));
        List<Task> initialTasks = taskRepository.findByProjectId(project.getId()).stream().filter(task -> task.getParentTasks().isEmpty()).collect(Collectors.toList());

        List<TaskDTO> startedInitialTasks = new ArrayList<>();

        for (Task task : initialTasks) {
            if (task.getStatus() == TaskStatus.PENDING) {
                task.setStatus(TaskStatus.STARTING);
                task.setStartedAt(LocalDateTime.now());
                Task startedTask = taskRepository.save(task);
                TaskDTO startedTaskDTO = modelMapper.map(startedTask, TaskDTO.class);
                startedInitialTasks.add(startedTaskDTO);
                createNotificationsForTask(task, "The task '" + task.getTaskName() + "' has started.");
            }
        }

        return startedInitialTasks;
    }


    public List<TaskDTO> getTasksByJobIdsStatusProjectPhaseStep(List<Long> jobIds, List<TaskStatus> statusList, Long projectId, Long phaseId, Long stepId) {

        List<Task> allTasks = taskRepository.findAll();

        List<TaskDTO> taskDTOs = allTasks.stream().filter(task -> task.getAssignedJobs().stream().anyMatch(job -> jobIds.contains(job.getId())) && statusList.contains(task.getStatus()) && (projectId == null || (task.getStep() != null && task.getStep().getPhase() != null && task.getStep().getPhase().getProject() != null && projectId.equals(task.getStep().getPhase().getProject().getId()))) && (phaseId == null || (task.getStep() != null && task.getStep().getPhase() != null && phaseId.equals(task.getStep().getPhase().getId()))) && (stepId == null || (task.getStep() != null && stepId.equals(task.getStep().getId())))).map(task -> modelMapper.map(task, TaskDTO.class)).collect(Collectors.toList());

        return taskDTOs;
    }


    public List<MethodExecutionDTO> getMethodExecutionsByTaskId(long taskId) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new EntityNotFoundException("Task with ID " + taskId + " not found."));

        List<MethodExecution> methodExecutions = task.getMethodExecutions();

        return methodExecutions.stream().map(methodExecution -> modelMapper.map(methodExecution, MethodExecutionDTO.class)).collect(Collectors.toList());
    }

    // Executes methods associated with a task and updates the task status
    public void executeMethodsForTask(long taskId, Map<Long, List<UserParameterDTO>> userParametersByMethodExecutionId) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new EntityNotFoundException("Task not found with ID: " + taskId));
        List<MethodExecution> methodExecutions = task.getMethodExecutions();

        // Iterate through the MethodExecution instances and attempt execution
        for (MethodExecution methodExecution : methodExecutions) {
            if (methodExecution.getStatus() != ExecutionStatus.SUCCESS && methodExecution.getStatus() != ExecutionStatus.IN_PROGRESS) {
                if (userParametersByMethodExecutionId.containsKey(methodExecution.getId())) {
                    executeMethodWithUserParameters(methodExecution, userParametersByMethodExecutionId);
                } else {
                    // Handle case when there is no matching key in the userParametersByMethodExecutionId map
                    System.out.println("No matching user parameters found for method execution ID: " + methodExecution.getId());
                }
            }
        }


        // If task does not have a child task, set its status to FINISHED, regardless of its verification requirement
        if (task.getChildTasks() == null || task.getChildTasks().isEmpty()) {
            task.setStatus(TaskStatus.FINISHED);
            task.setFinishedAt(LocalDateTime.now());
            // Start child tasks if they exist and are pending
            task.getChildTasks().forEach(childTask -> {
                if (childTask.getStatus() == TaskStatus.PENDING) {
                    childTask.setStatus(TaskStatus.STARTING);
                    childTask.setStartedAt(LocalDateTime.now());
                    createNotificationsForTask(childTask, "The task '" + childTask.getTaskName() + "' has started.");

                }
            });
        }
        // Otherwise, update task status based on whether it requires verification
        else {
            if (task.isRequiredVerification()) {
                task.setStatus(TaskStatus.WAITING_FOR_VALIDATION);
                createNotificationsForValidation(task, "Task " + task.getTaskName() + " is waiting for validation.");
            } else {
                task.setStatus(TaskStatus.FINISHED);
                task.setFinishedAt(LocalDateTime.now());
                // Start child tasks if they exist and are pending
                task.getChildTasks().forEach(childTask -> {
                    if (childTask.getStatus() == TaskStatus.PENDING) {
                        childTask.setStatus(TaskStatus.STARTING);
                        childTask.setStartedAt(LocalDateTime.now());
                        createNotificationsForTask(childTask, "The task '" + childTask.getTaskName() + "' has started.");

                    }
                });
            }
        }

        taskRepository.save(task);
    }


    // Attempts to execute a method with provided user parameters and updates the execution status
    private void executeMethodWithUserParameters(MethodExecution methodExecution, Map<Long, List<UserParameterDTO>> userParametersByMethodExecutionId) {
        Method method = methodExecution.getMethod();
        List<Parameter> parameters = method.getParameters();
        List<UserParameterDTO> userParameters = userParametersByMethodExecutionId.get(methodExecution.getId());


        // Validate all user parameters before attempting method execution
        if (areAllUserParametersValid(userParameters, parameters)) {
            try {
                methodExecution.setStatus(ExecutionStatus.IN_PROGRESS);
                executeMethod(method, userParameters);
                methodExecution.setStatus(ExecutionStatus.SUCCESS);

            } catch (Exception e) {
                System.out.println("Error executing method: " + method.getMethodName());
                methodExecution.setStatus(ExecutionStatus.FAILURE);
                e.printStackTrace();
            }
        } else {
            System.out.println("Invalid user parameters provided for method: " + method.getMethodName());
        }


        methodExecutionRepository.save(methodExecution);
    }


    // Checks if all user-provided parameters are valid for the given method parameters
    private boolean areAllUserParametersValid(List<UserParameterDTO> userParameters, List<Parameter> parameters) {
        return userParameters.stream().allMatch(userParam -> isValidUserParameter(userParam, parameters));
    }


    // Checks if a user-provided parameter is valid for the given method parameters
    private boolean isValidUserParameter(UserParameterDTO userParam, List<Parameter> parameters) {
        return parameters.stream().anyMatch(param -> param.getParameterName().equals(userParam.getParameterName()));
    }


    private void executeMethod(Method method, List<UserParameterDTO> userParameters) {
        java.lang.reflect.Method actionMethod;

        try {
            actionMethod = Arrays.stream(actionService.getClass().getDeclaredMethods()).filter(m -> m.getName().equals(method.getMethodName())).findFirst().orElseThrow(() -> new NoSuchMethodException("Method not found: " + method.getMethodName()));
        } catch (NoSuchMethodException e) {
            System.out.println("Method not found: " + method.getMethodName());
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        Object[] args = userParameters.stream().map(UserParameterDTO::getParameterValue).toArray();
        try {
            actionMethod.invoke(actionService, args);
        } catch (IllegalArgumentException e) {
            System.out.println("Illegal argument provided for method: " + method.getMethodName());
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            System.out.println("Error accessing method: " + method.getMethodName());
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            System.out.println("Error executing method: " + method.getMethodName());
            // Print the cause of the InvocationTargetException
            System.out.println("Cause: " + e.getCause());
            e.getCause().printStackTrace();
            throw new RuntimeException(e);
        }
    }

    // Validates a task and updates the status of its child tasks if necessary.
    public void validateAndStartChildTasks(long taskId) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new EntityNotFoundException("Task not found with ID: " + taskId));

        if (task.getStatus() != TaskStatus.WAITING_FOR_VALIDATION)
            throw new IllegalStateException("Task status must be WAITING_FOR_VALIDATION to validate.");

        task.setStatus(TaskStatus.FINISHED);
        task.setFinishedAt(LocalDateTime.now());

        // Start child tasks if their status is PENDING
        task.getChildTasks().forEach(childTask -> {
            if (childTask.getStatus() == TaskStatus.PENDING) {
                childTask.setStatus(TaskStatus.STARTING);
                childTask.setStartedAt(LocalDateTime.now());
                createNotificationsForTask(childTask, "Task" + task.getTaskName() + " is starting.");
            }
        });

        taskRepository.save(task);
    }

    public void invalidateTask(long taskId) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new EntityNotFoundException("Task not found with ID: " + taskId));
        System.out.println("TASK STATUS : " + task.getStatus());
        System.out.println("TASK id : " + task.getId());
        if (task.getStatus() != TaskStatus.WAITING_FOR_VALIDATION)
            throw new IllegalStateException("Task status must be WAITING_FOR_VALIDATION.");
        task.setStatus(TaskStatus.STARTING);
        task.setStartedAt(LocalDateTime.now());
        createNotificationsForTask(task, "The task '" + task.getTaskName() + "' has been invalidated. Please review.");
        taskRepository.save(task);
    }


    public List<TaskDTO> getTasksWaitingForValidationByJobIdAndProjectPhaseStep(long jobId, Long projectId, Long phaseId, Long stepId) {
        // Retrieve all tasks that are waiting for validation
        List<Task> tasks = taskRepository.findByStatus(TaskStatus.WAITING_FOR_VALIDATION);

        // Filter tasks by their child task's job
        List<Task> filteredTasks = tasks.stream().filter(task -> task.getChildTasks().stream().anyMatch(childTask -> childTask.getAssignedJobs().stream().anyMatch(job -> job.getId() == jobId))).collect(Collectors.toList());

        // Apply filters for projectId, phaseId, and stepId if they are not null
        List<Task> filteredTasksByProjectPhaseStep = filteredTasks.stream().filter(task -> projectId == null || (task.getStep() != null && task.getStep().getPhase() != null && task.getStep().getPhase().getProject() != null && projectId.equals(task.getStep().getPhase().getProject().getId()))).filter(task -> phaseId == null || (task.getStep() != null && task.getStep().getPhase() != null && phaseId.equals(task.getStep().getPhase().getId()))).filter(task -> stepId == null || (task.getStep() != null && stepId.equals(task.getStep().getId()))).collect(Collectors.toList());

        // Convert to DTOs
        List<TaskDTO> taskDTOs = filteredTasksByProjectPhaseStep.stream().map(task -> modelMapper.map(task, TaskDTO.class)).collect(Collectors.toList());

        return taskDTOs;
    }

    public TaskDTO addFeedback(long taskId, @Valid FeedbackDTO feedbackDTO) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task with ID " + taskId + " not found."));

        User user = userRepository.findById(feedbackDTO.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User with ID " + feedbackDTO.getUserId() + " not found."));

        Feedback feedback = modelMapper.map(feedbackDTO, Feedback.class);
        feedback.setTask(task);
        feedback.setUser(user);
        feedback.setFeedbackDate(LocalDateTime.now());
        feedbackRepository.save(feedback);

        // Add the feedback to the task's list of feedbacks
        task.getFeedbacks().add(feedback);

        Task updatedTask = taskRepository.save(task);
        return modelMapper.map(updatedTask, TaskDTO.class);
    }

    private void createNotificationsForTask(Task task, String message) {
        for (Job job : task.getAssignedJobs()) {
            for (User user : job.getUsers()) {
                // Create a new notification for each user assigned to the job
                Notification notification = new Notification();
                notification.setTask(task);
                notification.setUser(user);
                notification.setMessage(message);
                notification.setRead(false);

                // Save the notification to the database
                notificationRepository.save(notification);
            }
        }
    }


    private void createNotificationsForValidation(Task task, String message) {
        if (task.getChildTasks() != null) {
            for (Task childTask : task.getChildTasks()) {
                for (Job job : childTask.getAssignedJobs()) {
                    for (User user : job.getUsers()) {
                        // Create a new notification for each user assigned to the job of the child task
                        Notification notification = new Notification();
                        notification.setTask(childTask);
                        notification.setUser(user);
                        notification.setMessage(message);
                        notification.setRead(false);

                        // Save the notification to the database
                        notificationRepository.save(notification);
                    }
                }
            }
        }
    }

}