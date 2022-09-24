package downloaders;

import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.downloader.YoutubeCallback;
import com.github.kiulian.downloader.downloader.request.RequestPlaylistInfo;
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.model.playlist.PlaylistInfo;
import com.github.kiulian.downloader.model.playlist.PlaylistVideoDetails;
import util.Constants;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PlaylistDownload {

    private final String url;

    public PlaylistDownload(String url) {
        this.url = url;
    }

    /**
     * @return if this url is valid
     */
    public boolean isValidUrl(){
        if(url.startsWith(Constants.ytPrefix)) {
            return url.contains(Constants.playlistPrefix);
        }
        return false;
    }

    /**
     * checks for valid url
     *
     * @return video id within the link
     * @throws IllegalArgumentException for invalid url
     */
    public String idFromUrl() {
        if(isValidUrl()) {
            int idStart = url.indexOf(Constants.playlistPrefix) + Constants.playlistPrefix.length();
            int idEnd = url.indexOf("&", idStart);
            if(idEnd != -1) {
                return url.substring(idStart, idEnd);
            }
            return url.substring(idStart);
        } else {
            throw new IllegalArgumentException("not a valid Url");
        }
    }

    /**
     * requests playlist info
     *
     * @return Info to the playlist
     * @throws IllegalArgumentException for invalid url
     * @throws RuntimeException for failed request
     */
    public PlaylistInfo requestPlaylistInfo() {
        YoutubeDownloader downloader = new YoutubeDownloader();

        RequestPlaylistInfo request = new RequestPlaylistInfo(idFromUrl())
                .callback(new YoutubeCallback<PlaylistInfo>() {
                    @Override
                    public void onFinished(PlaylistInfo playlistInfo) {
                        System.out.println("finished");
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        System.out.println(throwable.getMessage());
                    }
                }).async();
        Response<PlaylistInfo> response = downloader.getPlaylistInfo(request);

        return response.data();
    }

    /**
     * downloads every video of the playlist
     *
     * @param path where the videos shall be downloaded to, creates an extra dir
     * @return a list of all files downloaded
     */
    public List<File> downloadPlaylistAudio(String path) {
        List<File> list = new ArrayList<>();

        PlaylistInfo info = requestPlaylistInfo();
        for(PlaylistVideoDetails details: info.videos()){
            Download download = new Download(Constants.ytPrefix + Constants.vidPrefix + details.videoId());

            list.add(download.downloadAudio(path + "\\" + info.details().title()));
        }
        return list;
    }
}
