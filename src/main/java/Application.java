import downloaders.PlaylistDownload;

import java.io.IOException;

public class Application {

    public static void main(String[] args) throws IOException {
      PlaylistDownload d = new PlaylistDownload("https://www.youtube.com/playlist?list=PLBwefvAJjA3772l82M4wRvXXKxP9YiKr_");
       d.downloadPlaylistAudio("");
       d.printToFile("");
    }
}
