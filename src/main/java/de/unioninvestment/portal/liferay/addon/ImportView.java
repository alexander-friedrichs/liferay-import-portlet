package de.unioninvestment.portal.liferay.addon;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;

import com.liferay.portal.NoSuchRoleException;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.User;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.RoleLocalServiceUtil;
import com.liferay.portal.service.RoleServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;

import de.unioninvestment.portal.liferay.addon.base.jaxb.LiferayBaselineDefinition;
import de.unioninvestment.portal.liferay.addon.base.jaxb.Roles;
import de.unioninvestment.portal.liferay.addon.content.jaxb.LiferayContentDescriptor;
import de.unioninvestment.portal.liferay.addon.userrole.jaxb.LiferayUserRoleDefinition;
import de.unioninvestment.portal.liferay.addon.userrole.jaxb.Users;

public class ImportView extends VerticalLayout {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(ImportView.class.getName());

	public ImportView() {
		final TextArea taImportXML = new TextArea();
		taImportXML.setHeight("200px");

		final String ROLLEN = "Rollen import";
		final String STRUKTUR = "Struktur import";
		final String USERROLEREAD = "UserRollen export";
		final String USERROLEWRITE = "UserRollen import";

		final ComboBox select = new ComboBox("Mode");

		select.addItem(ROLLEN);
		select.addItem(STRUKTUR);
		select.addItem(USERROLEREAD);
		select.addItem(USERROLEWRITE);

		final Button btImport = new Button("Execute");
		btImport.addClickListener(new ClickListener() {

			private static final long serialVersionUID = 1L;

			public void buttonClick(ClickEvent event) {

				String mode = (String) select.getValue();
				String xmltext = taImportXML.getValue().toString();

				ByteArrayInputStream input = new ByteArrayInputStream(xmltext
						.getBytes());

				try {
					if (mode.equalsIgnoreCase(STRUKTUR)) {

						importStruktur(input);

					} else if (mode.equalsIgnoreCase(ROLLEN)) {
						importRollen(input);

					} else if (mode.equalsIgnoreCase(USERROLEREAD)) {
						exportUserRoles(taImportXML);

					} else if (mode.equalsIgnoreCase(USERROLEWRITE)) {
						importUserRoles(input);
					}
				} catch (Exception e) {
					logger.log(Level.SEVERE, e.toString());
					e.printStackTrace();
				}

			}

		});
		addComponent(select);
		addComponent(taImportXML);
		addComponent(btImport);

		setExpandRatio(taImportXML, 1f);
	}

	private void importUserRoles(ByteArrayInputStream input) {
		LiferayUserRoleDefinition lurd = null;
		try {

			JAXBContext jaxbContext = JAXBContext
					.newInstance(LiferayUserRoleDefinition.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

			Reader reader = new InputStreamReader(input);
			lurd = (LiferayUserRoleDefinition) jaxbUnmarshaller
					.unmarshal(reader);

			Company company = CompanyLocalServiceUtil.getCompanyByWebId(lurd.getWebId());
			long companyId = company.getCompanyId();
			Users us = lurd.getUsers();
			List<de.unioninvestment.portal.liferay.addon.userrole.jaxb.User> users = us
					.getUser();

			for (de.unioninvestment.portal.liferay.addon.userrole.jaxb.User user : users) {
				User u = UserLocalServiceUtil.fetchUserByScreenName(companyId,
						user.getScreenName());
				if (u != null) {
					logger.log(Level.INFO, "user " + u.getScreenName());
					List<Role> strRoles = u.getRoles();

					long roleids[] = new long[strRoles.size()];
					int i = 0;
					for (Role strRole : strRoles) {
						Role r = RoleLocalServiceUtil.getRole(
								company.getCompanyId(), strRole.getName());
						if (r != null) {
							logger.log(
									Level.INFO,
									u.getScreenName() + " add role "
											+ r.getName() + " id "
											+ r.getRoleId());
							roleids[i] = r.getRoleId();
							i++;
						} else
							logger.log(Level.SEVERE, "Role does not exist - "
									+ strRole);
					}
					RoleLocalServiceUtil.addUserRoles(u.getUserId(), roleids);
				} else
					logger.log(Level.SEVERE,
							"User does not exist - " + user.getScreenName());
			}
		} catch (Exception e1) {
			logger.log(Level.SEVERE, e1.toString());
		}
	}

	@SuppressWarnings("unchecked")
	private void exportUserRoles(final TextArea taImportXML)
			throws SystemException, JAXBException, PropertyException {
		DynamicQuery query = DynamicQueryFactoryUtil.forClass(User.class);
		List<User> us = UserLocalServiceUtil.dynamicQuery(query);
		LiferayUserRoleDefinition lurd = new LiferayUserRoleDefinition();
		lurd.setWebId("union-investment.de");
		List<de.unioninvestment.portal.liferay.addon.userrole.jaxb.User> users = new ArrayList<de.unioninvestment.portal.liferay.addon.userrole.jaxb.User>();

		for (User u : us) {
			logger.log(Level.INFO, u.getScreenName());
			de.unioninvestment.portal.liferay.addon.userrole.jaxb.User user = new de.unioninvestment.portal.liferay.addon.userrole.jaxb.User();
			user.setScreenName(u.getScreenName());
			de.unioninvestment.portal.liferay.addon.userrole.jaxb.Roles rs = new de.unioninvestment.portal.liferay.addon.userrole.jaxb.Roles();
			List<Role> roles = u.getRoles();
			for (Role role : roles) {
				if (!role.getName().equalsIgnoreCase("User")
						&& !role.getName().equalsIgnoreCase("Power User")
						&& !role.getName().equalsIgnoreCase("Guest")) {
					de.unioninvestment.portal.liferay.addon.userrole.jaxb.Role ir = new de.unioninvestment.portal.liferay.addon.userrole.jaxb.Role();
					ir.setAction("ADD");
					ir.setValue(role.getName());
					rs.getRole().add(ir);
					logger.log(Level.INFO, " 	- " + role.getName());
				}
			}

			user.setRoles(rs);
			if (rs.getRole() != null) {
				if (rs.getRole().size() > 0)
					users.add(user);
			}
		}

		lurd.setUsers(new de.unioninvestment.portal.liferay.addon.userrole.jaxb.Users());
		lurd.getUsers().getUser().addAll(users);
		JAXBContext jaxbContext = JAXBContext
				.newInstance(LiferayUserRoleDefinition.class);
		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		StringWriter sw = new StringWriter();

		jaxbMarshaller.marshal(lurd, sw);
		taImportXML.setValue(sw.toString());
	}

	private void importRollen(ByteArrayInputStream input)
			throws PortalException, SystemException {
		LiferayBaselineDefinition lbDef = null;
		try {

			JAXBContext jaxbContext = JAXBContext
					.newInstance(LiferayBaselineDefinition.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

			Reader reader = new InputStreamReader(input);
			lbDef = (LiferayBaselineDefinition) jaxbUnmarshaller
					.unmarshal(reader);

		} catch (Exception e1) {
			logger.log(Level.SEVERE, e1.toString());
		}

		if (lbDef.getRoles() != null) {
			Roles rs = lbDef.getRoles();
			List<String> roles = rs.getRole();
			Company company = CompanyLocalServiceUtil.getCompanyByWebId(lbDef
					.getWebId());
			for (String role : roles) {
				try {
					RoleLocalServiceUtil.getRole(company.getCompanyId(), role);
					logger.log(Level.INFO, role + " exist");
				} catch (NoSuchRoleException e) {
					RoleServiceUtil.addRole(role, null, null, 1);
					logger.log(Level.INFO, role + " created");

				}
			}
		}
	}

	private void importStruktur(ByteArrayInputStream input)
			throws JAXBException {
		LiferayContentDescriptor contentDescriptor = null;
		JAXBContext jaxbContext = JAXBContext
				.newInstance(LiferayContentDescriptor.class);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		try {
			Reader reader = new InputStreamReader(input);
			contentDescriptor = (LiferayContentDescriptor) jaxbUnmarshaller
					.unmarshal(reader);
			LiferayContentImporter imp = new LiferayContentImporter();
			imp.importLiferayContent(contentDescriptor);
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.toString());
			Notification.show(e.toString(), Type.ERROR_MESSAGE);
		}
	}
}
