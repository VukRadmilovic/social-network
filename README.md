
# Deploying App with Docker

## Step 1: Create a Docker Network

First, create a Docker network that allows containers to communicate with each other. Run the following command:

```bash
docker network create some-network
```

## Step 2: Start MySQL Container

Start a MySQL container with the necessary environment variables. Replace `my-secret-pw` and `social_network` with your desired MySQL root password and database name:

```bash
docker run --name=some-mysql --env=MYSQL_ROOT_PASSWORD=my-secret-pw --env=MYSQL_DATABASE=social_network --network some-network --volume=/var/lib/mysql --restart=no --runtime=runc -d mysql:8.1
```

## Step 3: Start MinIO server

Start a Minio Server container with the required environment variables. This will also create a bucket named `profile-pictures`:

```bash
docker run -p 9000:9000 -d --name minio-server --env MINIO_ROOT_USER="admin" --env MINIO_ROOT_PASSWORD="password" --env MINIO_DEFAULT_BUCKETS="profile-pictures" --network some-network bitnami/minio:latest
```

## Step 4: Upload default profile picture

Place the picture in the opt/bitnami/minio-client folder
Use the Minio Client (mc) to copy the default image to the `profile-pictures` bucket:
```bash
mc cp default.jpg local/profile-pictures
```

## Step 5: Start Play Framework App

Now, start your Play Framework application container, specifying the required environment variables and the port to expose. Replace the environment variables with your actual values:
```bash
docker run --name play-app --network some-network --env=AwsAccessKeyId=admin --env=AuthSecretKey=Ao921kdkedoekdopkO@jjasidjasidHUHWUF782174812 --env=AwsSecretAccessKey=password --env=PLAY_HTTP_PORT=8080 --env=APPLICATION_SECRET=MzU4NEZFRkQ3MTE3MTVGMjRERkFDNUUzMTYyQUE= --rm -p 8080:8080 praksa-vuk-radmilovic-backend -Dconfig.resource=production.conf
```

Your Play Framework application should now be accessible at http://localhost:8080.

