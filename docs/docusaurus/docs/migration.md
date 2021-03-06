---
id: migration
title: Flowable Migration Guide : Flowable or Activiti v5 to Flowable V6
---

### Introduction

This guide describes the various things that are needed when migrating from Flowable or Activiti v5.x to Flowable V6.

### Design goals

The design goals of version 6 are:

-   Complete backwards compatibility with version 5: database-wise, concept-wise and code-wise.

-   Rewrite of the core engine: direct execution of BPMN 2.0 (vs transformation to intermediate model)

-   Simpler and cleaner runtime execution data structure, where predictability of the structure is crucial

-   Decoupling of persistence layers for future alternative implementations

### Database migration

There is no database migration needed when moving from v5 to V6: the database schema is the same for v5 and V6, with some additional tables and columns. All data produced by Flowable v5 can simply remain in the database, even including active, in-flight process instances. When the V6 engine is started for the first time, it will do an automatic update to the schema (as with previous v5 versions). Besides some small schema changes, the main update is that the job table has been split up into job, timer job, suspended job and dead letter job tables.
Any timer jobs that are not yet due will be moved to the new timer table. Jobs with no more retries left will be moved to the dead letter job table, and jobs for suspended process instances will be moved to the suspended job table.

### Conceptual changes

The main reason for calling this Flowable version *6* is because the core engine has been completely rewritten. The way core engine operations are executed has changed completely, along with a direct execution of BPMN (in v5 there is an intermediate model). Also, the way runtime executions are represented (the *execution tree*) changed. In general, both areas have been simplified significantly, making execution simpler and clearer, while also making the writing of custom behavior easier and more accessible. An embedded v5 engine is available in V6 to ensure complete execution compatibility if desired.

In future articles, we???ll describe the inner workings of the engine in detail.

### Breaking changes

The following changes are breaking changes (i.e. there will likely be a compile error).

#### Package rename: org.activiti to org.flowable

All org.activiti packages have been renamed to org.flowable.

#### Activiti class name renaming

All class names that contained "Activiti" have been renamed, with Activiti replaced by Flowable.
For example, ActivitiEvent has been renamed to FlowableEvent, and ActivitiException has been renamed to FlowableException.

#### activiti.cfg.xml renamed to flowable.cfg.xml

The default configuration file that???s read when starting the Flowable Engine has been renamed from activiti.cfg.xml to flowable.cfg.xml.
The same is true for the default Spring configuration file activiti-context.xml, which has been renamed to flowable-context.xml.

#### PVM classes

All classes previously in the org.activiti.engine.impl.pvm package (and sub packages) have been removed. This is because the *PVM* (Process Virtual Machine) model has been replaced by a simpler and more lightweight model.

This means that usages of *ActivityImpl*, *ProcessDefinitionImpl*, *ExecutionImpl*, *TransitionImpl* are now invalid.

Most of the usage of these classes in v5 came down to getting information that was contained in the process definition. In V6, all the process definition information can be found through the *BpmnModel*, which is a Java representation of the BPMN 2.0 XML of the process definition (enhanced to make certain operations and searches easier).

The quickest way to get the *BpmnModel* for a process definition is to use the org.flowable.engine.RepositoryService service:

    BpmnModel bpmnModel = repositoryService.getBpmnModel(myProcessDefinitionId);

To get the *RepositoryService* a JavaDelegate or ActivityBehavior implementation class, use the org.flowable.engine.impl.context.Context class:

    RepositoryService repositoryService = Context.getProcessEngineConfiguration().getRepositoryService();

(Note that this only works when a context is active, like in a JavaDelegate or ActivityBehavior)

#### ActivityExecution is replaced by DelegateExecution

We removed ActivityExecution and replaced it, where used, with the DelegateExecution class.

All methods from the ActivityExecution class are copied to the DelegateExecution class.

#### EngineServices removed

We removed the getEngineServices method on DelegateExecution, because it had no real benefit and it made the usage of DelegateExecution in the Flowable 6 and the embedded Flowable 5 engine difficult.

Replace all getEngineServices method calls with org.flowable.engine.impl.context.Context.getProcessEngineConfiguration method calls.

#### Job, timer, suspended and dead letter jobs

Flowable v5 had only 1 job table and this meant that a fairly complex query had to be executed to get the jobs that needed to be executed from the database.

With Flowable V6, the jobs have been split up in job (ACT\_RU\_JOB), timer (ACT\_RU\_TIMER\_JOB), suspended (ACT\_RU\_SUSPENDED\_JOB), and (ACT\_RU\_DEADLETTER\_JOB) dead letter tables.
Jobs in the job table (such as asynchronous jobs and due timer jobs) can be directly executed. So there???s no need for a complex query anymore, the only *where* clause is a lock time column that should be NULL.
Timer jobs are now persisted in a dedicated timer jobs table and there???s a thread that checks for timer jobs due to execute. When a timer job is due to be executed, the job will be moved to the job table.
When the job executor Runnable is ready to execute the job, it will be fetched from the job table and executed.
When a process definition and process instance is suspended, the corresponding jobs will be moved to a separate suspended job table. This simplifies the job executor query and makes it very clear which jobs are suspended.
If a job execution fails, the job will be placed in the timer job table with a due date that???s set to current time + the async failed job wait time configured on the process engine configuration.
When the job is due to be executed, it will be moved to the job table again and be executed. When the number of retries is down to zero, the job will be moved to the dead letter table and no automatic execution will be performed.
This also simplifies the default job executor queries and makes it obvious which jobs are stuck and might need manual intervention.

The embedded Flowable v5 engine in Flowable V6 works with these 4 job tables as well. But there???s only one threadpool fetching jobs from the database, shared between the engines. When a job is fetched from the database, the engine version for the job is checked based on the process definition id, and the job is executed by the Flowable V6 or embedded Flowable v5 Engine.

#### Signaling an execution

In v5, there was always confusion about *signaling an execution* when using, for example, *runtimeService.signal(executionI);*. As a *signal* is a valid BPMN 2.0 concept and feature, it conflicts conceptually.

In V6, the *signal()* methods have been renamed to *trigger()*.

This also means that *SignalableActivityBehavior*, the interface to be implemented for behaviors that can be *triggered* from external sources, is now called *TriggerableActivityBehavior*.

#### Checked Exceptions

In v5, the delegate classes, such as *JavaDelegate* and *ActivityBehavior*, have *throws Exception* in their signatures. As with any modern framework, the use of checked Exceptions has been removed in V6.

#### Delegate classes

*org.flowable.engine.impl.pvm.delegate.ActivityBehavior* has changed package and lives now in *org.flowable.engine.impl.delegate*.

The following methods have been removed from *DelegateExecution*:

-   end()

-   createdExecution()

They have been replaced by calls to the ExecutionEntityManager, which can be fetched through Context.getCommandContext.getExecutionEntityManager().

#### EntityManagers

In Flowable v5, all EntityManager classes (responsible for persistence but also certain logic) did not have an interface. In V6, all EntityManager classes have been renamed to have *Impl* as suffix and an interface without the suffix. This effectively means that the v5 EntityManager class name is now the name of the corresponding interface.

All EntityManager interfaces extend the generic org.flowable.engine.impl.persistence.entity.EntityManager interface. All implementation classes extend a generic *AbstractEntityManager* interface.

Also, for consistency:
\* The UserIdentityManager interface has been renamed to UserEntityManager
\* The GroupIdentityManager interface has been renamed to GroupEntityManager

#### PersistentObject renamed to Entity

The class *org.flowable.engine.impl.db.PersistentObject* has been renamed to *Entity* to be consistent with all the other classes (EntityManagers and so on).

All related classes that used the term *persistent object* have been refactored to *entity* too.

#### Separation of identity logic and tables

In v5, the identity logic and tables were an integral part of the process engine. In V6, the logic has been refactored into a separate module called *flowable-idm-engine* (where IDM stands for 'identity management). The related database tables are managed by this engine. For backwards compatibility, the IDM engine is enabled by default when booting up the process engine. To disable the engine, set the *disableIdmEngine* to *true* in the process engine configuration. When disabled, no identity database tables (starting with *ACT\_ID*) will be created, or they can be removed if they already exist.

#### Camel endpoint renamed to flowable

When using the Flowable Camel module, make sure to use the flowable endpoint, instead of the activiti endpoint. The Route below provides a simple example:

    public class SimpleCamelCallRoute extends RouteBuilder {

      @Override
      public void configure() throws Exception {
        from("flowable:SimpleCamelCallProcess:simpleCall").to("log:org.flowable.camel.examples.SimpleCamelCall");
      }
    }

### V5 compatibility

When migrating to Flowable V6 (typically by replacing the JAR files on the classpath), all current deployments and process definitions are *tagged* as being *version 5* artifacts. At various points (completing a task, starting a new process instance, task assignment, ?????? quite a long list) the engine will check whether the associated process definition has the *version 5 tag*. If so, it will delegate execution to an *embedded compact version 5 engine*.

To eliminate migration, use of the embedded v5 engine allows a phase out approach: let any current process definitions run in *'version 5 mode* until you have verified and tested the behavior of your processes to be identical on V6.

By default the embedded v5 engine is *disabled*! To enable it, add the following to the engine config:

    <property name="flowable5CompatibilityEnabled" value="true" />

**and** add the **flowable5-compatibility** JAR to your classpath (manually or through a dependency mechanism, such as Maven).

In the rare case of the default implementation *org.flowable.compatibility.DefaultFlowable5CompatibilityHandler* being insufficient, a custom implementation can be created. To do this, set the *flowable5CompatibilityHandlerFactory* property of the engine configuration to the fully qualified classname. That Factory class should produce an instance of the handler responsible for bridging from version 6 to 5.

To move a v5 process definition to run on the V6 engine, simply redeploy it. New process instances will run in *version 6 mode*, while existing process instances will run in *version 5 mode*).

If, for some reason, you still want to deploy a new version of a process definition to run in *version 5 mode*, the following code can be used:

    repositoryService.createDeployment()
          .addClasspathResource("xyz")
          .deploymentProperty(DeploymentProperties.DEPLOY_AS_FLOWABLE5_PROCESS_DEFINITION, Boolean.TRUE)
          .deploy();

If you are using the Flowable Spring module, additional configuration is needed for Flowable v5 compatibility:

    <property name="flowable5CompatibilityEnabled" value="true" />
    <property name="flowable5CompatibilityHandlerFactory" ref="flowable5CompabilityFactory" />

    ....

    <bean id="flowable5CompabilityFactory" class="org.flowable.compatibility.spring.SpringFlowable5CompatibilityHandlerFactory" />

**and** add the **flowable5-spring** and **flowable5-spring-compatibility** JARs to your classpath (manually or through a dependency mechanism, such as Maven).
