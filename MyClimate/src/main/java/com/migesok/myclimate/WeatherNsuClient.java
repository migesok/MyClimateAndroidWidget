package com.migesok.myclimate;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.migesok.myclimate.IOUtils.closeQuietly;

public class WeatherNsuClient {
    public double getCurrentTemperature() throws IOException, XmlPullParserException {
        URL url = new URL("http://weather.nsu.ru/weather_brief.xml");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        InputStream responseStream = null;
        try {
            responseStream = new BufferedInputStream(url.openStream());
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(responseStream, "UTF-8");
            parser.nextTag();
            return readCurrentTemperature(parser);
        } finally {
            closeQuietly(responseStream);
            connection.disconnect();
        }
    }

    private double readCurrentTemperature(XmlPullParser parser)
            throws XmlPullParserException, IOException {

        parser.require(XmlPullParser.START_TAG, null, "weather");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() == XmlPullParser.START_TAG) {
                if ("current".equals(parser.getName())) {
                    return readCurrent(parser);
                } else {
                    skip(parser);
                }
            }
        }
        throw new IllegalArgumentException(
                "unable to find current temperature value in the server response");
    }

    private double readCurrent(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, "current");
        double current = Double.parseDouble(readText(parser));
        parser.require(XmlPullParser.END_TAG, null, "current");
        return current;
    }

    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = null;
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}
