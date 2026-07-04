package com.osrs.dps.ui;

import com.osrs.dps.data.ImageCache;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/** Small helpers for lazily loading wiki images into ImageViews. */
public final class Icons {

    private Icons() {
    }

    /** Creates an ImageView of the given size, lazily loaded from the wiki image name. */
    public static ImageView view(String imageName, double size) {
        ImageView view = new ImageView();
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(true);
        set(view, imageName);
        return view;
    }

    /** Loads the image into the view asynchronously (no-op for null/blank names). */
    public static void set(ImageView view, String imageName) {
        if (imageName == null || imageName.isBlank()) {
            view.setImage(null);
            return;
        }
        Image cached = ImageCache.cached(imageName);
        if (cached != null) {
            view.setImage(cached);
            return;
        }
        view.setImage(null);
        ImageCache.load(imageName, view::setImage);
    }
}
