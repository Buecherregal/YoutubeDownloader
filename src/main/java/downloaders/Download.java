package downloaders;

import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.downloader.YoutubeCallback;
import com.github.kiulian.downloader.downloader.request.RequestSubtitlesDownload;
import com.github.kiulian.downloader.downloader.request.RequestVideoFileDownload;
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo;
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.model.Extension;
import com.github.kiulian.downloader.model.playlist.PlaylistInfo;
import com.github.kiulian.downloader.model.subtitles.SubtitlesInfo;
import com.github.kiulian.downloader.model.videos.VideoDetails;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.Format;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp4.Mp4FileReader;
import org.jaudiotagger.audio.mp4.Mp4FileWriter;
import org.jaudiotagger.tag.FieldDataInvalidException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.mp4.Mp4FieldKey;
import org.jaudiotagger.tag.mp4.Mp4Tag;
import util.Constants;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Download {

    private String url;

    private VideoInfo info;

    public Download(String url) {
        info = null;
        this.url = url;
    }

    /**
     * @return if this url is valid
     */
    public boolean isValidUrl() {
        if(url.startsWith(Constants.ytPrefix)) {
            return url.contains(Constants.vidPrefix);
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
            int idStart = url.indexOf(Constants.vidPrefix) + Constants.vidPrefix.length();
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
     * requests video info
     *
     * @return Info to the Video
     * @throws IllegalArgumentException for invalid url
     * @throws RuntimeException for failed request
     */
    public VideoInfo requestVideoInfo() {
        YoutubeDownloader downloader = new YoutubeDownloader();
        String id = idFromUrl();

        RequestVideoInfo request = new RequestVideoInfo(id)
                .callback(new YoutubeCallback<VideoInfo>() {
                    @Override
                    public void onFinished(VideoInfo videoInfo) {
                        System.out.println("got video info");
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        System.out.println(throwable.getMessage());
                    }
                })
                .maxRetries(5)
                .async();
        Response<VideoInfo> response = downloader.getVideoInfo(request);
        if(response.data() == null) {
            throw new RuntimeException("could not get video info for video: " + url);
        }
        return response.data();
    }

    /**
     * downloads the audio of the video in best audio format
     * validates url
     *
     * @param path video gets saved to
     * @return the resulting audio-file (most likely .m4a), named as video-title
     * @throws IllegalArgumentException for invalid url
     */
    public File downloadAudio(String path) {
        if(info == null) {
            info = requestVideoInfo();
        }
        Format f = info.bestAudioFormat();
        YoutubeDownloader downloader = new YoutubeDownloader();

        String fileName = info.details().title();

        RequestVideoFileDownload request = new RequestVideoFileDownload(f)
                .saveTo(new File(path))
                .renameTo(info.details().title())
                .overwriteIfExists(true);
        downloader.downloadVideoFile(request);

        return new File(path + fileName + "." + request.getFormat().extension().value());
    }

    /**
     * downloads the video with audio in the best format
     * validates url
     *
     * @param path video gets saved to
     * @return the resulting video-file (most likely .mp4), named as video-title
     * @throws IllegalArgumentException for invalid url
     */
    public File downloadVideoAndAudio(String path) {
        if(info == null) {
            info = requestVideoInfo();
        }
        Format f = info.bestVideoWithAudioFormat();
        YoutubeDownloader downloader = new YoutubeDownloader();

        RequestVideoFileDownload request = new RequestVideoFileDownload(f)
                .saveTo(new File(path))
                .renameTo(info.details().title())
                .overwriteIfExists(true);
        downloader.downloadVideoFile(request);

        return new File(path + info.details().title() + "." + request.getFormat().extension().value());
    }

    /**
     * downloads the video in the specified format
     * validates url
     *
     * @param path video gets saved to
     * @param format desired format (may be audio/video only)
     * @return the resulting video-file, named as video-title
     * @throws IllegalArgumentException for invalid url
     */
    public File download(String path, Extension format) {
        File file = new File(path);
        Format f = filterFormat(format);
        YoutubeDownloader downloader = new YoutubeDownloader();

        if(info == null) {
            info = requestVideoInfo();
        }

        RequestVideoFileDownload request = new RequestVideoFileDownload(f)
                .saveTo(file)
                .renameTo(info.details().title())
                .overwriteIfExists(true);
        downloader.downloadVideoFile(request);

       return new File(path + info.details().title() + "." + request.getFormat().extension().value());
    }

    /**
     * filters all available formats
     *
     * @param ex desired format
     * @return the Format with Extension ex
     * @throws RuntimeException if format not found
     * @throws IllegalArgumentException for invalid url
     */
    private Format filterFormat(Extension ex) {
        if(info == null) {
            info = requestVideoInfo();
        }
        List<Format> formats = info.findFormats(format -> format.extension() == ex);
        if(formats.isEmpty()) {
            throw new RuntimeException("could not find format");
        }
        return formats.get(0);
    }

    public void addMetadata(String lang, File file) throws TagException, CannotReadException, InvalidAudioFrameException, ReadOnlyFileException, IOException, CannotWriteException {
        if(info == null) {
            requestVideoInfo();
        }
        VideoDetails details = info.details();

        Mp4FileReader reader = new Mp4FileReader();
        AudioFile audio = reader.read(file);
        Mp4FileWriter writer = new Mp4FileWriter();

        Mp4Tag mTag = (Mp4Tag) audio.getTag();
        mTag.addField(mTag.createField(Mp4FieldKey.ARTIST, details.author()));
        mTag.addField(mTag.createField(Mp4FieldKey.TITLE, details.title()));

        writer.write(audio);
        //TODO tag=null atm fix - cant create default tag da kein writer für m4a writer hat keine berechtigung - use Tag statt M4aTag
    }

    public void addPlaylistMeta(PlaylistInfo info, File file) throws FieldDataInvalidException {
        AudioFile audio = new AudioFile();
        audio.setFile(file);
        Tag tag = audio.getTag();

        tag.setField(FieldKey.ALBUM, info.details().title());
        tag.setField(FieldKey.ALBUM_ARTIST, info.details().author());
        //TODO same as add metadata
    }

    /**
     * @param lang desired subtitle language, may not be available
     * @return subtitles formatted as plain text
     * @throws RuntimeException if no subtitles in that langauge are found
     */
    public String getSubtitles(String lang) {
        System.out.println("reading subtitles");
        if(info == null) {
            info = requestVideoInfo();
        }
        for(SubtitlesInfo subtitlesInfo: info.subtitlesInfo()) {
            if(subtitlesInfo.getLanguage().equals(lang)) {
                String subtitles = requestSubtitles(subtitlesInfo);
                return formatSubtitles(subtitles);
            }
        }
        throw new RuntimeException("could not find subtitles");
    }

    /**
     * requests the subtitles
     *
     * @param info of subtitles
     * @return a TTML containing subtitles
     */
    public String requestSubtitles(SubtitlesInfo info) {
        YoutubeDownloader downloader = new YoutubeDownloader();

        RequestSubtitlesDownload request = new RequestSubtitlesDownload(info).formatTo(Extension.TTML);
        Response<String> response = downloader.downloadSubtitle(request);

        return response.data();
    }

    /**
     * filters the text out of a TTML file
     *
     * @param subtitles as TTML
     * @return only the text
     */
    public String formatSubtitles(String subtitles) {
        String[] lines = subtitles.split("\n");

        StringBuilder formatted = new StringBuilder();

        for(String line: lines) {
            if(line.startsWith("<p")) {
               int iStart = line.indexOf(">") + 1;
               int iEnd = line.indexOf("</p>");
               formatted.append(line, iStart, iEnd).append("\n");
            }
        }
        return formatted.toString();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public VideoInfo getInfo() {
        return info;
    }

    public void setInfo(VideoInfo info) {
        this.info = info;
    }
}
