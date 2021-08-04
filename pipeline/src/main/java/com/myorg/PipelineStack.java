package com.myorg;

import java.util.Collections;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.SecretValue;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.codebuild.BuildEnvironment;
import software.amazon.awscdk.services.codebuild.BuildEnvironmentVariable;
import software.amazon.awscdk.services.codebuild.PipelineProject;
import software.amazon.awscdk.services.codebuild.PipelineProjectProps;
import software.amazon.awscdk.services.codepipeline.Artifact;
import software.amazon.awscdk.services.codepipeline.Pipeline;
import software.amazon.awscdk.services.codepipeline.PipelineProps;
import software.amazon.awscdk.services.codepipeline.StageOptions;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildAction;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildActionProps;
import software.amazon.awscdk.services.codepipeline.actions.GitHubSourceAction;
import software.amazon.awscdk.services.codepipeline.actions.GitHubTrigger;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.s3.Bucket;
import static software.amazon.awscdk.services.codebuild.LinuxBuildImage.AMAZON_LINUX_2;

public class PipelineStack extends Stack {
    
	public PipelineStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public PipelineStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // The code that defines your stack goes here
        Bucket artifactsBucket = new Bucket(this, "ArtifactsBucket");
        
        
        IRepository codeRepo = Repository.fromRepositoryName(this, "AppRepository", "sam-app");

        Pipeline pipeline = new Pipeline(this, "Pipeline", PipelineProps.builder()
                .artifactBucket(artifactsBucket).build());

        Artifact sourceOutput = new Artifact("sourceOutput");
        
        //https://docs.aws.amazon.com/codepipeline/latest/userguide/reference-pipeline-structure.html

       
        GitHubSourceAction gitCommitSource = GitHubSourceAction.Builder.create()
        		.actionName("GitCommit_Source")
        		.repo("aws-cdi")
        		.branch("master")
        		.output(sourceOutput)
        		.owner("ychampagne")
        		.trigger(GitHubTrigger.POLL)
        		.oauthToken(new SecretValue("sometoken value not used"))
        		.build();
        		
        		
        		
        pipeline.addStage(StageOptions.builder()
                .stageName("Source")
                .actions(Collections.singletonList(gitCommitSource))
                .build());
        
        
     // Declare build output as artifacts
        Artifact buildOutput = new Artifact("buildOutput");

        // Declare a new CodeBuild project
        PipelineProject buildProject = new PipelineProject(this, "Build", PipelineProjectProps.builder()
                .environment(BuildEnvironment.builder()
                        .buildImage(AMAZON_LINUX_2).build())
                .environmentVariables(Collections.singletonMap("PACKAGE_BUCKET", BuildEnvironmentVariable.builder()
                        .value(artifactsBucket.getBucketName())
                        .build()))
                .build());

        // Add the build stage to our pipeline
        CodeBuildAction buildAction = new CodeBuildAction(CodeBuildActionProps.builder()
                .actionName("Build")
                .project(buildProject)
                .input(sourceOutput)
                .outputs(Collections.singletonList(buildOutput))
                .build());

        pipeline.addStage(StageOptions.builder()
                .stageName("Build")
                .actions(Collections.singletonList(buildAction))
                .build());
    }
}
