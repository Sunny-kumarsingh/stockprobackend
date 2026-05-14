# StockPro EC2 Deployment

Jenkins builds backend images and pushes them to Docker Hub. EC2 pulls those images and runs them with `docker-compose.ec2.yml`.

## Jenkins setup

Create a Jenkins credential:

- Type: `Username with password`
- ID: `dockerhub-credentials`
- Username: your Docker Hub username
- Password: Docker Hub access token

If your Docker Hub namespace is not `sunnykumarsingh`, update `DOCKERHUB_NAMESPACE` in `Jenkinsfile`.

## EC2 first-time setup

Install Docker and Docker Compose plugin on EC2, then copy these files to the server:

- `docker-compose.ec2.yml`
- `.env`

Do not commit `.env`. Keep it only on EC2.

## Deploy on EC2

Run from the directory containing `docker-compose.ec2.yml` and `.env`:

```bash
docker compose -f docker-compose.ec2.yml --env-file .env pull
docker compose -f docker-compose.ec2.yml --env-file .env up -d --remove-orphans
```

## Update after a new Jenkins build

```bash
docker compose -f docker-compose.ec2.yml --env-file .env pull
docker compose -f docker-compose.ec2.yml --env-file .env up -d --remove-orphans
docker image prune -f
```

## Logs

```bash
docker compose -f docker-compose.ec2.yml logs -f api-gateway
docker compose -f docker-compose.ec2.yml ps
```
