package de.unioninvestment.portal.liferay.addon;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

@SuppressWarnings("serial")
@Theme("liferay62")
public class LiferayImportPortlet extends UI {

	@Override
	protected void init(VaadinRequest request) {
		setContent(new ImportView());
	}

}
