from .app import make_app
import os
import tornado.ioloop


if __name__ == "__main__":
    app = make_app()
    port = int(os.environ.get("PORT", "8080"))
    app.listen(port)
    print(f"Tornado server running on http://0.0.0.0:{port}")
    tornado.ioloop.IOLoop.current().start()