package org.activityinfo.i18n.tools;

import com.google.common.base.Strings;
import org.activityinfo.i18n.tools.model.ResourceClassTerm;
import org.activityinfo.i18n.tools.model.Term;
import org.activityinfo.i18n.tools.model.TranslationSet;
import org.activityinfo.i18n.tools.model.TranslationSource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

//import org.activityinfo.i18n.tools.po.PoEditorClient;
//import org.activityinfo.i18n.tools.po.PoTerm;
//import org.activityinfo.i18n.tools.po.PoTermUpdate;
//import org.activityinfo.i18n.tools.po.PoUploadResponse;

/**
 * Defines the source of the translations as a PoEditor.com project
 */
public class PoEditorSource implements TranslationSource {
    private int projectId;
    private String apiToken;

    public PoEditorSource(int projectId, String apiToken) {
        this.projectId = projectId;
        this.apiToken = apiToken;
        if(Strings.isNullOrEmpty(this.apiToken)) {
            throw new IllegalArgumentException("API Token is missing.");
        }
    }

    public int getProjectId() {
        return projectId;
    }

    public String getApiToken() {
        return apiToken;
    }

    @Override
    public Map<String, ? extends Term> fetchTerms() throws IOException {
//        PoEditorClient client = new PoEditorClient(apiToken);
//        return client.getTerms(projectId);
        return null;
    }

    @Override
    public TranslationSet fetchTranslations(String language) throws IOException {
//        PoEditorClient client = new PoEditorClient(apiToken);
//        return client.getTranslations(projectId, language);
        return null;
    }


    /**
     * Updates the translation source, adding any missing terms and their default
     * translations.
     * 
     * @param terms
     */
    public void updateTerms(List<ResourceClassTerm> terms) throws IOException {
        
//        List<PoTermUpdate> updates = Lists.newArrayList();
//        for (ResourceClassTerm term : terms) {
//            updates.add(new PoTermUpdate(term.getKey(), term.getDefaultTranslation()));
//        }
//
//        PoEditorClient client = new PoEditorClient(apiToken);
//        PoUploadResponse response = client.upload(projectId, updates);
//
//        PoUploadResponse.Details details = response.getDetails();
//        System.out.println(String.format("Terms:       %5d  Added: %5d  Deleted: %d",
//                details.getTerms().getParsed(),
//                details.getTerms().getAdded(),
//                details.getTerms().getDeleted()));
//
//        System.out.println(String.format("Definitions: %5d  Added: %5d  Updated: %d",
//                details.getDefinitions().getParsed(),
//                details.getDefinitions().getAdded(),
//                details.getDefinitions().getUpdated()));
    }

    @Override
    public String toString() {
        return "PoEditor.com[projectId=" + projectId + "]";
    }


}
