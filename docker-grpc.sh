#!/bin/bash

# LogPilot gRPC-only Docker Build and Run Script
# This script builds the Docker image and runs the LogPilot application in gRPC-only mode

set -e  # Exit on any error

# Configuration
IMAGE_NAME="logpilot-grpc"
TAG="latest"
CONTAINER_NAME="logpilot-grpc-app"
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
    print_info "Building gRPC-only Docker image: ${IMAGE_NAME}:${TAG}"

    if docker build -f Dockerfile.grpc -t ${IMAGE_NAME}:${TAG} .; then
        print_success "gRPC-only Docker image built successfully"
    else
        print_error "Failed to build gRPC-only Docker image"
        exit 1
    fi
}

# Function to run container
run_container() {
    print_info "Starting gRPC-only container: ${CONTAINER_NAME}"
    print_info "gRPC service will be available at: localhost:${GRPC_PORT}"

    docker run -d \
        --name ${CONTAINER_NAME} \
        -p ${GRPC_PORT}:50051 \
        -e SPRING_PROFILES_ACTIVE=grpc \
        -e LOGPILOT_PROTOCOL=grpc \
        ${IMAGE_NAME}:${TAG}

    if [ $? -eq 0 ]; then
        print_success "gRPC-only container started successfully"
        print_info "Container name: ${CONTAINER_NAME}"
        print_info "Use 'docker logs ${CONTAINER_NAME}' to view logs"
        print_info "Use 'docker stop ${CONTAINER_NAME}' to stop the application"
    else
        print_error "Failed to start gRPC-only container"
        exit 1
    fi
}

# Function to wait for application to be ready
wait_for_app() {
    print_info "Waiting for gRPC service to be ready..."

    # Wait up to 60 seconds for the application to start
    for i in {1..60}; do
        # Check if grpcurl is available and test the service
        if command -v grpcurl >/dev/null 2>&1; then
            if grpcurl -plaintext localhost:${GRPC_PORT} grpc.health.v1.Health/Check >/dev/null 2>&1; then
                print_success "gRPC service is ready!"
                return 0
            fi
        else
            # Fallback: check if port is open
            if nc -z localhost ${GRPC_PORT} >/dev/null 2>&1; then
                print_success "gRPC service port is open!"
                print_warning "Install grpcurl for better health checking: brew install grpcurl"
                return 0
            fi
        fi
        echo -n "."
        sleep 1
    done

    print_warning "Application may still be starting. Check logs with: docker logs ${CONTAINER_NAME}"
}

# Function to test gRPC service
test_grpc() {
    print_info "Testing gRPC service..."

    # Check if grpcurl is available
    if ! command -v grpcurl >/dev/null 2>&1; then
        print_warning "grpcurl not found. Install it to test gRPC endpoints:"
        echo "  macOS: brew install grpcurl"
        echo "  Linux: apt-get install grpcurl or download from GitHub"
        return
    fi

    # Test health check
    if grpcurl -plaintext localhost:${GRPC_PORT} grpc.health.v1.Health/Check >/dev/null 2>&1; then
        print_success "gRPC health check is working"
    else
        print_warning "gRPC health check not responding"
    fi

    # Show available services
    print_info "Available gRPC services:"
    grpcurl -plaintext localhost:${GRPC_PORT} list 2>/dev/null || print_warning "Could not list services"

    # Show some example gRPC calls
    echo ""
    print_info "=== Example gRPC Usage ==="
    echo "  List services:"
    echo "    grpcurl -plaintext localhost:${GRPC_PORT} list"
    echo ""
    echo "  Health check:"
    echo "    grpcurl -plaintext localhost:${GRPC_PORT} grpc.health.v1.Health/Check"
    echo ""
    echo "  Send log via gRPC (example):"
    echo "    grpcurl -plaintext -d '{\"logEntry\":{\"channel\":\"test\",\"level\":\"INFO\",\"message\":\"Hello from gRPC\"}}' \\"
    echo "      localhost:${GRPC_PORT} com.logpilot.grpc.proto.LogPilotService/StoreLog"
    echo ""
}

# Function to show application info
show_info() {
    echo ""
    print_info "=== LogPilot gRPC-only Application Info ==="
    print_info "gRPC Service: localhost:${GRPC_PORT}"
    print_info "Container: ${CONTAINER_NAME}"
    print_info "Mode: gRPC-only (REST disabled)"
    echo ""
    print_info "=== Useful Commands ==="
    echo "  View logs:     docker logs ${CONTAINER_NAME}"
    echo "  Follow logs:   docker logs -f ${CONTAINER_NAME}"
    echo "  Stop app:      docker stop ${CONTAINER_NAME}"
    echo "  Remove app:    docker rm ${CONTAINER_NAME}"
    echo "  Shell access:  docker exec -it ${CONTAINER_NAME} /bin/bash"
    echo ""
    print_info "=== gRPC Tools ==="
    echo "  Install grpcurl: brew install grpcurl (macOS) or apt-get install grpcurl (Linux)"
    echo "  Test health:     grpcurl -plaintext localhost:${GRPC_PORT} grpc.health.v1.Health/Check"
    echo "  List services:   grpcurl -plaintext localhost:${GRPC_PORT} list"
    echo ""
}

# Main execution
main() {
    print_info "Starting LogPilot gRPC-only Docker build and run process..."

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
            test_grpc
            show_info
            ;;
        "build-run"|"")
            build_image
            cleanup_container
            run_container
            wait_for_app
            test_grpc
            show_info
            ;;
        "stop")
            print_info "Stopping LogPilot gRPC-only application..."
            docker stop ${CONTAINER_NAME} >/dev/null 2>&1 || true
            print_success "Application stopped"
            ;;
        "clean")
            print_info "Cleaning up LogPilot gRPC-only Docker resources..."
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
        "test")
            test_grpc
            ;;
        "help"|"-h"|"--help")
            echo "LogPilot gRPC-only Docker Build and Run Script"
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
            echo "  test         Test gRPC service endpoints"
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