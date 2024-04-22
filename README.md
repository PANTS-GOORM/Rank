# WordSketch Rank 서비스

> TEAM PANTS

<!-- ## Devcontainer로 빠르게 환경 구축하기 (추가 예정)
[![Open in Dev Containers](https://img.shields.io/static/v1?label=Dev%20Containers&message=Open&color=blue&logo=visualstudiocode)](https://vscode.dev/redirect?url=vscode://ms-vscode-remote.remote-containers/cloneInVolume?url=https://github.com/PANTS-GOORM/BackEnd) -->

## 프로젝트 실행하는 법

### 1. Repository와 Submodule 전체를 Clone

```bash
git clone --recursive https://github.com/PANTS-GOORM/Rank.git
```

### 1-1. 만약 git clone을 먼저 한 상태라면, secrets 파일들을 submodule로 불러오기

```bash
git submodule init
git submodule update
```

### 3. 로컬 또는 컨테이너 환경에서 BackEnd Server와 Redis 실행하기

- [BackEnd 서버 실행과정](https://github.com/PANTS-GOORM/BackEnd)

- Redis 실행 과정 (아래에 기술)

> 로컬 환경에서 실행할 경우

1. 로컬에서 [Redis](https://redis.io/downloads/) 7.0 stable 버전을 설치합니다.
2. 설치 후, 터미널에서 redis-cli를 실행해 설치 상태를 확인합니다.

> 컨테이너 환경에서 실행할 경우

1. [Docker Desktop](https://www.docker.com/products/docker-desktop/)을 설치합니다.
2. CLI 환경에서 명령어를 통해 컨테이너를 실행합니다.

```bash
docker run --name WordSketch-Redis -p 6379:6379 -d redis:7.0
```

3. 실행된 컨테이너의 내부에서 redis-cli를 실행해 설치 상태를 확인합니다.

```bash
# Redis 컨터이너 내부 접속
docker exec -it WordSketch-Redis /bin/bash

# Redis 실행 확인
redis-cli
```

### 4. 로컬에서 Rank 서비스 실행

- IDE를 통해 실행하기
- JRE를 통해 실행하기

```bash
# 프로젝트 루트 디렉토리로 이동
cd Rank

# Test를 제외하고 Gradle로 빌드
./gradlew -x test clean build

# JRE를 통해 서비스 실행
java -jar build/libs/*.jar
```
