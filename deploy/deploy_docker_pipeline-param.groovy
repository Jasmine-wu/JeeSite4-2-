pipeline {
    agent {
        label 'MyNewNode'
    }

    parameters {
        string(name: 'branch', defaultValue: 'master', description: 'Git branch')
        choice(name: 'env_type', choices: ['prod', 'qa'], description: 'Environment Type')
    }

    environment {
        docker_image = 'jeesite4'
        docker_container = 'iJeesite4'
        env = "$params.env_type"
    }

    stages{
        stage('添加系统的环境变量') {
            steps {
                script{
                    def dockerPath = tool 'docker' //全局配置里的docker
                    env.PATH = "${dockerPath}:${env.PATH}" //添加了系统环境变量上
                }
            }
        }
        stage('同步源码') {
            steps {
                git url:'git@github.com:Jasmine-wu/JeeSite4-2-.git', branch:"$params.branch"
            }
        }

        stage('设定配置文件'){
            steps{
                sh '''#!/bin/bash
                    . ~/.bash_profile 
                    
                    if [[ "${env}" == "prod" ]]; then
                        export mysql_ip=${mysql_prod_ip}
                        export mysql_port=${mysql_prod_port}
                    else
                        export mysql_ip=${mysql_qa_ip}
                        export mysql_port=${mysql_qa_port}
                    fi
                    
                    export os_type=`uname`
                    cd ${WORKSPACE}/web/bin/docker
                    if [[ "${os_type}" == "Darwin" ]]; then
                        sed -i "" "s/mysql_ip/${mysql_docker_ip}/g" application-${env}.yml
                        sed -i "" "s/mysql_port/${mysql_docker_port}/g" application-${env}.yml
                        sed -i "" "s/mysql_user/${mysql_user}/g" application-${env}.yml
                        sed -i "" "s/mysql_pwd/${mysql_pwd}/g" application-${env}.yml
                        sed -i "" "s/<env>/${env}/g" Dockerfile-param
                    else
                        sed -i "s/mysql_ip/${mysql_docker_ip}/g" application-${env}.yml
                        sed -i "s/mysql_port/${mysql_docker_port}/g" application-${env}.yml
                        sed -i "s/mysql_user/${mysql_user}/g" application-${env}.yml
                        sed -i "s/mysql_pwd/${mysql_pwd}/g" application-${env}.yml
                        sed -i "s/<env>/${env}/g" Dockerfile-param
                    fi
                '''
            }
        }

        stage('Maven 编译'){
            steps {
                sh '''#!/bin/bash
                    . ~/.bash_profile
                    
                    cd ${WORKSPACE}/root
                    mvn clean install -Dmaven.test.skip=true
                    
                    cd ${WORKSPACE}/web
                    mvn clean package spring-boot:repackage -Dmaven.test.skip=true -U
                '''
            }
        }

        stage('停止 / 删除 现有Docker Container/Image '){
            steps {
                script{
                    try{
                        sh 'docker stop ${docker_container}-${env}'
                    }catch(exc){
                        echo "The container ${docker_container}-${env} does not exist"
                    }

                    try{
                        sh 'docker rm ${docker_container}-${env}'
                    }catch(exc){
                        echo "The container ${docker_container}-${env} does not exist"
                    }

                    try{
                        sh 'docker rmi ${docker_image}-${env}'
                    }catch(exc){
                        echo "The docker image ${docker_image}-${env} does not exist"
                    }
                }
            }
        }

        stage('生成新的Docker Image'){
            steps {
                sh '''#!/bin/bash
                    cd ${WORKSPACE}/web/bin/docker
                    rm -f web.war
                    cp ${WORKSPACE}/web/target/web.war .
                    docker build -t ${docker_image}-${env} -f Dockerfile-param .
                '''
            }
        }

        stage('启动新Docker实例'){
            steps {
                sh '''#!/bin/bash
                    if [[ "${env}" == "prod" ]]; then
                        export port="8899"
                    else
                        export port="8811"
                    fi
                    
                    docker run -d --name ${docker_container}-${env} -p ${port}:8980 ${docker_image}-${env}
                '''
            }
        }
    }
}