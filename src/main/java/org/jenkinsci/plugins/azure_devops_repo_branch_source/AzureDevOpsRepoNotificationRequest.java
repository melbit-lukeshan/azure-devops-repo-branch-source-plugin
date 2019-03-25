/*
 * The MIT License
 *
 * Copyright 2017 Steven Foster
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.azure_devops_repo_branch_source;

import org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model.GitStatusState;

/**
 * Details of a Azure DevOps status notification to be sent.
 * One AzureDevOpsRepoNotificationRequest represents one notification. A strategy supplies a list of these to request one or more
 * notifications.
 * Notifications are differentiated by their Context label. If two notification requests with the same Context label are
 * provided, one will override the other.
 *
 * @see <a href="https://developer.github.com/v3/repos/statuses/">Github API</a> for details of the purpose of each notification field.
 * @since TODO
 */
public class AzureDevOpsRepoNotificationRequest {

    private final String context;
    private final String url;
    private final String message;
    private final GitStatusState state;
    private final boolean ignoreError;

    /**
     * @since TODO
     */
    private AzureDevOpsRepoNotificationRequest(String context, String url, String message, GitStatusState state, boolean ignoreError) {
        this.context = context;
        this.url = url;
        this.message = message;
        this.state = state;
        this.ignoreError = ignoreError;
    }

    public static AzureDevOpsRepoNotificationRequest build(String context, String url, String message, GitStatusState state, boolean ignoreError) {
        return new AzureDevOpsRepoNotificationRequest(context, url, message, state, ignoreError);
    }

    /**
     * Returns the context label to be used for a notification
     *
     * @return context
     * @since TODO
     */
    public String getContext() {
        return context;
    }

    /**
     * Returns the URL to be supplied with a notification
     *
     * @return url
     * @since TODO
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns the message for a notification
     *
     * @return message
     * @since TODO
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the commit state of a notification
     *
     * @return state
     * @since TODO
     */
    public GitStatusState getState() {
        return state;
    }

    /**
     * Returns whether the notification processor should ignore errors when interacting with GitHub
     *
     * @return ignoreError
     * @since TODO
     */
    public boolean isIgnoreError() {
        return ignoreError;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "AzureDevOpsRepoNotificationRequest{" +
                "context='" + context + '\'' +
                ", url='" + url + '\'' +
                ", message='" + message + '\'' +
                ", state=" + state +
                ", ignoreError=" + ignoreError +
                '}';
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AzureDevOpsRepoNotificationRequest that = (AzureDevOpsRepoNotificationRequest) o;

        if (ignoreError != that.ignoreError) return false;
        if (context != null ? !context.equals(that.context) : that.context != null) return false;
        if (url != null ? !url.equals(that.url) : that.url != null) return false;
        if (message != null ? !message.equals(that.message) : that.message != null) return false;
        return state == that.state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = context != null ? context.hashCode() : 0;
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (state != null ? state.hashCode() : 0);
        result = 31 * result + (ignoreError ? 1 : 0);
        return result;
    }
}
