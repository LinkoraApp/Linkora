config.devServer = config.devServer || {};
config.devServer.host = '0.0.0.0';
config.devServer.port = 61252;
config.devServer.headers = config.devServer.headers || {};
config.devServer.headers["Cross-Origin-Opener-Policy"] = "same-origin";
config.devServer.headers["Cross-Origin-Embedder-Policy"] = "require-corp";