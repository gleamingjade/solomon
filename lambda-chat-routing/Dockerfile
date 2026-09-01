FROM public.ecr.aws/lambda/nodejs:20

COPY package*.json ./
RUN npm ci --omit=dev

COPY src/ ./src/

# Default entry point used in deployment (AWS Lambda). Locally, docker-compose overrides
# this via command to run src/local-server.js instead.
CMD ["src/lambda-entry.handler"]
