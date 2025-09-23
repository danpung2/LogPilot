#!/bin/bash

# LogPilot Docker Build and Run Script
# This script builds the Docker image and runs the LogPilot application

set -e  # Exit on any error

# Configuration
IMAGE_NAME="logpilot"
TAG="latest"
CONTAINER_NAME="logpilot-app"
REST_PORT="8080"
GRPC_PORT="50051"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if Docker is running
check_docker() {
    if ! docker info >/dev/null 2>&1; then
        print_error "Docker is not running. Please start Docker and try again."
        exit 1
    fi
}

# Function to stop and remove existing container
cleanup_container() {
    if docker ps -a --format 'table {{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
        print_info "Stopping and removing existing container: ${CONTAINER_NAME}"
        docker stop ${CONTAINER_NAME} >/dev/null 2>&1 || true
        docker rm ${CONTAINER_NAME} >/dev/null 2>&1 || true
        print_success "Container cleanup completed"
    fi
}

# Function to build Docker image
build_image() {
    print_info "Building Docker image: ${IMAGE_NAME}:${TAG}"

    if docker build -t ${IMAGE_NAME}:${TAG} .; then
        print_success "Docker image built successfully"
    else
        print_error "Failed to build Docker image"
        exit 1
    fi
}

# Function to run container
run_container() {
    print_info "Starting container: ${CONTAINER_NAME}"
    print_info "REST API will be available at: http://localhost:${REST_PORT}"
    print_info "gRPC service will be available at: localhost:${GRPC_PORT}"

    docker run -d \
        --name ${CONTAINER_NAME} \
        -p ${REST_PORT}:8080 \
        -p ${GRPC_PORT}:50051 \
        ${IMAGE_NAME}:${TAG}

    if [ $? -eq 0 ]; then
        print_success "Container started successfully"
        print_info "Container name: ${CONTAINER_NAME}"
        print_info "Use 'docker logs ${CONTAINER_NAME}' to view logs"
        print_info "Use 'docker stop ${CONTAINER_NAME}' to stop the application"
    else
        print_error "Failed to start container"
        exit 1
    fi
}

# Function to wait for application to be ready
wait_for_app() {
    print_info "Waiting for application to be ready..."

    # Wait up to 60 seconds for the application to start
    for i in {1..60}; do
        if curl -f http://localhost:${REST_PORT}/actuator/health >/dev/null 2>&1; then
            print_success "Application is ready!"
            print_info "Health check: http://localhost:${REST_PORT}/actuator/health"
            return 0
        fi
        echo -n "."
        sleep 1
    done

    print_warning "Application may still be starting. Check logs with: docker logs ${CONTAINER_NAME}"
}

# Function to show application info
show_info() {
    echo ""
    print_info "=== LogPilot Application Info ==="
    print_info "REST API: http://localhost:${REST_PORT}"
    print_info "gRPC Service: localhost:${GRPC_PORT}"
    print_info "Health Check: http://localhost:${REST_PORT}/actuator/health"
    print_info "Container: ${CONTAINER_NAME}"
    echo ""
    print_info "=== Useful Commands ==="
    echo "  View logs:     docker logs ${CONTAINER_NAME}"
    echo "  Follow logs:   docker logs -f ${CONTAINER_NAME}"
    echo "  Stop app:      docker stop ${CONTAINER_NAME}"
    echo "  Remove app:    docker rm ${CONTAINER_NAME}"
    echo "  Shell access:  docker exec -it ${CONTAINER_NAME} /bin/bash"
    echo ""
}

# Main execution
main() {
    print_info "Starting LogPilot Docker build and run process..."

    # Check if Docker is available
    check_docker

    # Parse command line arguments
    case "${1:-build-run}" in
        "build")
            build_image
            ;;
        "run")
            cleanup_container
            run_container
            wait_for_app
            show_info
            ;;
        "build-run"|"")
            build_image
            cleanup_container
            run_container
            wait_for_app
            show_info
            ;;
        "stop")
            print_info "Stopping LogPilot application..."
            docker stop ${CONTAINER_NAME} >/dev/null 2>&1 || true
            print_success "Application stopped"
            ;;
        "clean")
            print_info "Cleaning up LogPilot Docker resources..."
            cleanup_container
            docker rmi ${IMAGE_NAME}:${TAG} >/dev/null 2>&1 || true
            print_success "Cleanup completed"
            ;;
        "logs")
            docker logs ${CONTAINER_NAME}
            ;;
        "follow-logs")
            docker logs -f ${CONTAINER_NAME}
            ;;
        "help"|"-h"|"--help")
            echo "LogPilot Docker Build and Run Script"
            echo ""
            echo "Usage: $0 [COMMAND]"
            echo ""
            echo "Commands:"
            echo "  build-run    Build image and run container (default)"
            echo "  build        Build Docker image only"
            echo "  run          Run container only"
            echo "  stop         Stop the running container"
            echo "  clean        Stop container and remove image"
            echo "  logs         Show container logs"
            echo "  follow-logs  Follow container logs"
            echo "  help         Show this help message"
            echo ""
            ;;
        *)
            print_error "Unknown command: $1"
            print_info "Use '$0 help' for usage information"
            exit 1
            ;;
    esac
}

# Run main function with all arguments
main "$@"