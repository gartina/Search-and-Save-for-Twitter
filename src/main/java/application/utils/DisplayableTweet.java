package application.utils;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class DisplayableTweet {

	private /*ObjectProperty<LocalDateTime>*/StringProperty createdAt;
	private StringProperty author;
	private StringProperty text;
	private boolean retweet;
	// TODO crear parámetro nuevo: link del tweet
	
	public DisplayableTweet(String c, String a, String t, boolean r) {
		this.createdAt = new SimpleStringProperty(c);//new SimpleObjectProperty<LocalDateTime>(string);
		this.author = new SimpleStringProperty(a);
		this.text = new SimpleStringProperty(t);
		this.retweet = r;
	}
	
	public String getCreatedAt() {
        return createdAt.get();
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt.set(createdAt);
    }

    public StringProperty createdAtProperty() {
        return createdAt;
    }
    
    public String getAuthor() {
		return author.get();
	}
	
	public void setAuthor(String author) {
		this.author.set(author);
	}
	
	public StringProperty authorProperty() {
		return author;
	}
	
	public String getTweetText() {
		return text.get();
	}
	
	public void setTweetText(String text) {
		this.text.set(text);
	}
	
	public StringProperty tweetTextProperty() {
		return text;
	}
	
	public boolean getRetweet() {
		return retweet;
	}
	
	public void setRetweet(boolean retweet) {
		this.retweet = retweet;
	}
}
