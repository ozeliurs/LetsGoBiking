FROM mcr.microsoft.com/dotnet/sdk:7.0.404-alpine3.18
WORKDIR /app
COPY . .
RUN dotnet dev-certs https
EXPOSE 5229
ENTRYPOINT ["dotnet", "run"]